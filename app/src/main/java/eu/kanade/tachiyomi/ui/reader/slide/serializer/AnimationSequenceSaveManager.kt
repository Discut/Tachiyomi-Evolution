package eu.kanade.tachiyomi.ui.reader.slide.serializer

import android.content.Context
import android.os.Handler
import android.os.Looper
import eu.kanade.tachiyomi.data.orm.GalleryDatabase
import eu.kanade.tachiyomi.data.orm.models.AnimationSequence
import eu.kanade.tachiyomi.ui.reader.slide.engine.SlideAnimationPath
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * 动画序列保存管理器
 *
 * 负责管理动画序列的自动保存和加载功能，采用延迟保存策略：
 * 1. 编辑停止后延迟保存（默认 3 秒）
 * 2. 支持手动触发立即保存
 * 3. 支持加载已保存的动画序列
 *
 * 使用示例：
 * ```
 * val saveManager = AnimationSequenceSaveManager(context)
 * saveManager.setCurrentAnimation(imageId, animationPath)
 * saveManager.markAsDirty()  // 标记为已修改，触发延迟保存
 * saveManager.saveImmediately()  // 立即保存
 * val loadedPath = saveManager.load(imageId)  // 加载已保存的动画
 * saveManager.dispose()  // 释放资源
 * ```
 */
class AnimationSequenceSaveManager(
    private val context: Context,
    private val saveDelayMs: Long = DEFAULT_SAVE_DELAY_MS,
) {
    // ============================================
    // 内部状态
    // ============================================

    private val saveHandler = Handler(Looper.getMainLooper())
    private var saveRunnable: Runnable? = null
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private var isDirty = false
    private var currentImageId: Long? = null
    private var currentAnimationPath: SlideAnimationPath? = null

    // ============================================
    // 公开 API
    // ============================================

    /**
     * 设置当前正在编辑的动画序列
     *
     * @param imageId 图片 ID
     * @param animationPath 动画路径
     */
    fun setCurrentAnimation(imageId: Long?, animationPath: SlideAnimationPath?) {
        currentImageId = imageId
        currentAnimationPath = animationPath
    }

    /**
     * 获取当前设置的动画路径
     */
    fun getCurrentAnimationPath(): SlideAnimationPath? = currentAnimationPath

    /**
     * 获取当前设置的图片 ID
     */
    fun getCurrentImageId(): Long? = currentImageId

    /**
     * 标记当前动画为已修改，触发延迟保存
     *
     * 如果有待执行的保存任务，会先取消，然后重新开始计时
     */
    fun markAsDirty() {
        isDirty = true
        saveRunnable?.let { saveHandler.removeCallbacks(it) }

        saveRunnable = Runnable {
            saveImmediately()
            isDirty = false
        }
        saveHandler.postDelayed(saveRunnable!!, saveDelayMs)
    }

    /**
     * 立即保存当前动画序列到数据库
     *
     * 如果当前没有设置动画或图片 ID，则不执行保存
     */
    fun saveImmediately() {
        val path = currentAnimationPath ?: return
        val imageId = currentImageId ?: return

        scope.launch(Dispatchers.IO) {
            try {
                saveToDatabase(imageId, path)
                withUIContext {
                    context.toast("关键帧序列已存储")
                }
            } catch (e: Exception) {
                withUIContext {
                    // 可选：显示错误提示
                    Timber.e(e, "Failed to save animation sequence")
                    context.toast("保存失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 检查是否有未保存的修改
     */
    fun hasUnsavedChanges(): Boolean = isDirty

    /**
     * 从数据库加载指定图片的动画序列
     *
     * @param imageId 图片 ID
     * @return 动画路径，如果不存在则返回 null
     */
    fun load(imageId: Long): SlideAnimationPath? {
        return try {
            val db = GalleryDatabase.getDatabase(context)
            val dao = db.getAnimationSequenceDao()
            val sequence = dao.getFirstByImageId(imageId)

            if (sequence != null) {
                AnimationSequenceSerializer.deserialize(sequence.data)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 删除指定图片的所有动画序列
     *
     * @param imageId 图片 ID
     */
    suspend fun deleteByImageId(imageId: Long): Int {
        val db: GalleryDatabase = Injekt.get()
        val dao = db.getAnimationSequenceDao()
        return dao.deleteByImageId(imageId)
    }

    /**
     * 获取指定图片的动画序列数量
     *
     * @param imageId 图片 ID
     * @return 动画序列数量
     */
    fun countByImageId(imageId: Long): Int {
        return try {
            val db = GalleryDatabase.getDatabase(context)
            val dao = db.getAnimationSequenceDao()
            dao.countByImageId(imageId)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 释放资源
     *
     * 应在 Activity/Fragment 销毁时调用
     */
    fun dispose() {
        saveRunnable?.let { saveHandler.removeCallbacks(it) }
        saveRunnable = null
        job.cancel()
    }

    // ============================================
    // 内部方法
    // ============================================

    /**
     * 保存动画序列到数据库
     */
    private suspend fun saveToDatabase(
        imageId: Long,
        path: SlideAnimationPath,
    ) {
        val db = GalleryDatabase.getDatabase(context)
        val dao = db.getAnimationSequenceDao()

        // 序列化动画路径
        val json = AnimationSequenceSerializer.serialize(path)

        // 检查是否已存在该图片的动画序列
        val existing = dao.getFirstByImageId(imageId)

        if (existing != null) {
            // 更新现有序列
            val updated = existing.copy(
                durationMs = path.durationMs,
                data = json,
                updatedAt = System.currentTimeMillis(),
            )
            dao.update(updated)
        } else {
            // 创建新序列
            val sequence = AnimationSequence(
                imageId = imageId,
                name = "Animation_${System.currentTimeMillis()}",
                durationMs = path.durationMs,
                data = json,
                version = 1,
            )
            dao.insert(sequence)
        }
    }

    companion object {
        /** 默认延迟保存时间：3 秒 */
        const val DEFAULT_SAVE_DELAY_MS = 3000L
    }
}
