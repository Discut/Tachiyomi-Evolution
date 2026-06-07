package eu.kanade.tachiyomi.data.tagger

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Environment
import android.os.Handler
import android.os.Looper
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger

/**
 * WD14 图像打标器，全局单例（通过 Injekt DI 注入）。
 *
 * - 构造时仅加载标签表（轻量），模型在首次推理时懒加载
 * - 提供 [predict]（阻塞）和 [predictAsync]（suspend）两种推理路径
 * - 分层内存压力响应：MODERATE 级即开始回收，而非等到 CRITICAL
 * - 空闲超时回收：N 秒无推理自动释放 GPU 资源，下次推理时自动热加载
 */
class WDTagger(private val context: Context) {

    // ========== 构造时完成：标签表、缓冲区、内存回调注册 ==========

    private val modelTags: List<ModelTag> = loadTags(context)

    // 预分配预处理缓冲区（~3MB，复用整套推理生命周期）
    private val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * 3 * 448 * 448).apply {
        order(ByteOrder.nativeOrder())
    }
    private val floatView = inputBuffer.asFloatBuffer()
    private val floatBuffer = FloatArray(448 * 448 * 3)
    private val pixels = IntArray(448 * 448)

    // NSFW 检测：预计算中文名集合
    private val nsfwNamesCn: Set<String> by lazy {
        modelTags.filter { it.name in listOf("explicit", "questionable", "sensitive") }
            .map { it.nameCn }
            .toSet()
    }

    // ========== 懒加载状态 ==========

    @Volatile private var interpreter: Interpreter? = null

    @Volatile private var isLoaded = false
    private val loadLock = Any()

    // 活跃推理计数：>0 时禁止内存回收
    private val activeInferences = AtomicInteger(0)

    // ========== 空闲超时回收 ==========

    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleTimeoutMs = 60_000L // 60s 无推理自动释放

    private val idleRunnable = Runnable {
        if (activeInferences.get() == 0 && isLoaded) {
            Timber.i("[WDTagger] 空闲超时 %dms，释放模型", idleTimeoutMs)
            releaseModel()
        }
    }

    private fun resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, idleTimeoutMs)
    }

    // ========== 内存感知 ==========

    private val memoryCallback = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            if (activeInferences.get() > 0 || !isLoaded) return
            when (level) {
                // 系统还正常运行但可用内存已偏紧 — 最佳回收窗口
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
                // 系统严重缺内存，再不放可能触发 OOM killer
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                // App 进入后台，释放 GPU 资源给前台 App
                ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
                // 接近被杀，能放的全放
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
                -> {
                    Timber.w("[WDTagger] 内存压力 level=%d，释放模型", level)
                    releaseModel()
                }
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration) {}

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {
        }
    }

    init {
        context.applicationContext.registerComponentCallbacks(memoryCallback)
    }

    // ========== 公共 API ==========

    /**
     * 阻塞推理。首次调用自动触发模型懒加载。必须在后台线程调用。
     */
    fun predict(bitmap: Bitmap): List<Pair<String, Float>> {
        ensureLoaded()
        resetIdleTimer()
        activeInferences.incrementAndGet()
        try {
            return doPredict(bitmap)
        } finally {
            activeInferences.decrementAndGet()
        }
    }

    /**
     * Suspend 推理，自动调度到 [Dispatchers.Default]。
     */
    suspend fun predictAsync(bitmap: Bitmap): List<Pair<String, Float>> {
        return withContext(Dispatchers.Default) {
            predict(bitmap)
        }
    }

    /**
     * NSFW 检测快捷方法
     */
    fun isNSFW(scores: List<Pair<String, Float>>): Boolean {
        return scores.any { it.first in nsfwNamesCn }
    }

    /**
     * 释放模型资源（解释器 + GPU delegate），下次推理时自动重新加载。
     * 仅在没有活跃推理时生效。
     */
    fun releaseModel() {
        synchronized(loadLock) {
            if (activeInferences.get() > 0) return
            idleHandler.removeCallbacks(idleRunnable)
            interpreter?.close()
            interpreter = null
            isLoaded = false
            Timber.i("[WDTagger] 模型已释放")
        }
    }

    /**
     * 完全销毁：释放模型 + 取消内存回调。
     */
    fun close() {
        context.applicationContext.unregisterComponentCallbacks(memoryCallback)
        releaseModel()
    }

    // ========== 模型加载 ==========

    /**
     * 懒加载模型，double-checked locking 保证线程安全。
     */
    private fun ensureLoaded() {
        if (isLoaded) return
        synchronized(loadLock) {
            if (isLoaded) return

            val modelFile = findModelFile()
            val start = System.currentTimeMillis()

            val modelBytes: MappedByteBuffer = loadFromFile(modelFile)

            interpreter = try {
                Interpreter(
                    modelBytes,
                    Interpreter.Options().apply {
                        addDelegate(GpuDelegate())
                    },
                ).also {
                    Timber.i("[WDTagger] GPU 加速可用")
                }
            } catch (e: Throwable) {
                Timber.e(e, "[WDTagger] GPU 加速不可用")
                Interpreter(
                    modelBytes,
                    Interpreter.Options().apply {
                        setNumThreads(4)
                    },
                ).also {
                    Timber.i("[WDTagger] CPU 模式 (XNNPack, 4线程)")
                }
            }

            isLoaded = true
            Timber.i("[WDTagger] 模型加载完成，耗时: %dms", System.currentTimeMillis() - start)
        }
    }

    private fun findModelFile(): File {
        // 1. 私有目录
        val internalFile = File(context.filesDir, "models/$MODEL_NAME")
        if (internalFile.exists()) {
            Timber.i("[WDTagger] 使用私有模型: ${internalFile.absolutePath}")
            return internalFile
        }

        // 2. 外部公共目录（TachiyomiJ2K/models/）
        val externalDir = File(
            Environment.getExternalStorageDirectory(),
            context.getString(R.string.app_name) + File.separator + "models",
        ).also { it.mkdirs() }
        val externalFile = File(externalDir, MODEL_NAME)
        if (externalFile.exists()) {
            Timber.i("[WDTagger] 使用外部模型: ${externalFile.absolutePath}")
            return externalFile
        }

        throw FileNotFoundException(
            "模型文件未找到，请将 $MODEL_NAME 放入 " +
                externalDir.absolutePath,
        )
    }

    private fun loadFromFile(file: File): MappedByteBuffer {
        val inputStream = FileInputStream(file)
        return inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            0,
            file.length(),
        )
    }

    // ========== 推理核心 ==========

    private fun doPredict(bitmap: Bitmap): List<Pair<String, Float>> {
        val tTotal = System.currentTimeMillis()

        // 1. 预处理
        preprocess(bitmap)
        val tPreprocess = System.currentTimeMillis()

        // 2. 推理
        val scoresArray = Array(1) { FloatArray(modelTags.size) }
        interpreter!!.run(inputBuffer, scoresArray)
        val scores = scoresArray[0]
        val tInfer = System.currentTimeMillis()

        // 3. 后处理
        val threshold = 0.35f
        val result = modelTags.mapIndexedNotNull { index, tag ->
            if (scores[index] > threshold) {
                tag.nameCn to scores[index]
            } else {
                null
            }
        }.sortedByDescending { it.second }
        val tPost = System.currentTimeMillis()

        Timber.i(
            "[WDTagger] 总耗时: %dms | 预处理: %dms | 推理: %dms | 后处理: %dms | 标签数: %d",
            tPost - tTotal,
            tPreprocess - tTotal,
            tInfer - tPreprocess,
            tPost - tInfer,
            result.size,
        )
        return result
    }

    // ========== 预处理 ==========

    private fun preprocess(bitmap: Bitmap) {
        val t0 = System.currentTimeMillis()

        val resized = if (bitmap.width == 448 && bitmap.height == 448) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, 448, 448, true)
        }
        val tResize = System.currentTimeMillis()

        resized.getPixels(pixels, 0, 448, 0, 0, 448, 448)
        val tGetPixels = System.currentTimeMillis()
        if (resized !== bitmap) resized.recycle()

        // NHWC BGR 布局（WD14 模型 OpenCV 训练惯例）
        var fi = 0
        for (h in 0 until 448) {
            val row = h * 448
            for (w in 0 until 448) {
                val pixel = pixels[row + w]
                floatBuffer[fi++] = (pixel and 0xFF).toFloat() // B
                floatBuffer[fi++] = ((pixel shr 8) and 0xFF).toFloat() // G
                floatBuffer[fi++] = ((pixel shr 16) and 0xFF).toFloat() // R
            }
        }
        val tConvert = System.currentTimeMillis()

        floatView.rewind()
        floatView.put(floatBuffer)
        inputBuffer.rewind()
        val tPut = System.currentTimeMillis()

        Timber.i(
            "[WDTagger.pre] resize: %dms | getPixels: %dms | NHWC转换: %dms | putFloat批量: %dms | 同尺寸跳过: %b",
            tResize - t0,
            tGetPixels - tResize,
            tConvert - tGetPixels,
            tPut - tConvert,
            bitmap.width == 448 && bitmap.height == 448,
        )
    }

    companion object {
        private const val MODEL_NAME = "model_perfect_stripped.tflite"
    }
}
