package eu.kanade.tachiyomi.data.gallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.model.toSImage
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

// 1. 定义处理流程
fun processImageBatches(
    jobList: List<ImageBO>,
    batchSize: Int,
    maxConcurrentBatches: Int = 2,
): Flow<ProcessResult> = channelFlow {
    val imageCache = Injekt.get<ImageCache>()

    val sourceManager = Injekt.get<SourceManager>()

    val semaphore = Semaphore(maxConcurrentBatches)

    jobList.chunked(batchSize).forEachIndexed { index, batch ->
        semaphore.acquire()
        launch {
            try {
                val results = batch.map { image ->
                    async(Dispatchers.Default) {
                        try {
                            val sourceImage = withIOContext {
                                sourceManager.getGallerySource(image.source)
                                    ?.getImageStream(image.dbImage.toSImage())
                            }
                                ?: throw Exception("Image not found, current image: ${image.dbImage.filePath}")

                            // CPU 计算（如滤镜处理）
                            val lowQualityImage = compressImageStream(
                                sourceImage.originStream,
                                image.dbImage.width / 5,
                                image.dbImage.height / 5,
                            )

                            withContext(Dispatchers.IO) {
                                // IO 操作（如保存文件）
                                imageCache.put(
                                    image.id.toString(),
                                    ByteArrayInputStream(lowQualityImage),
                                )
                            }
                            ProcessedResult.Success(image.id.toString())
                        } catch (e: Exception) {
                            ProcessedResult.Failure(image.id.toString(), e)
                        }
                    }
                }.awaitAll()

                if (results.any { it is ProcessedResult.Failure }) {
                    send(ProcessResult.Error(Exception("Failed to generate thumbnail"), index))
                    results.forEach {
                        Timber.e(results.toString())
                    }
                } else {
                    send(ProcessResult.BatchComplete(index, results))
                }
            } catch (e: Exception) {
                send(ProcessResult.Error(e, index))
            } finally {
                semaphore.release()
            }
        }
    }
}.buffer(Channel.UNLIMITED).catch { e -> emit(ProcessResult.Error(e)) }

// 2. 数据模型
sealed class ProcessResult {
    data class BatchComplete(val batchId: Int, val results: List<ProcessedResult>) : ProcessResult()
    data class Error(val exception: Throwable, val batchId: Int? = null) : ProcessResult()
}

sealed class ProcessedResult {
    data class Success(val imageId: String) : ProcessedResult()
    data class Failure(val imageId: String, val error: Throwable) : ProcessedResult()
}

fun decodeSampledBitmap(inputStream: InputStream, reqWidth: Int, reqHeight: Int): Bitmap? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    // BitmapFactory.decodeStream(inputStream, null, options)
    // inputStream.reset() // 重置流以便重新解码

    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeStream(inputStream, null, options)
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val (height, width) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun compressBitmap(
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
): ByteArray {
    val outputStream = ByteArrayOutputStream()
    var quality = 90 // 初始质量值
    bitmap.compress(format, quality, outputStream)

    // 动态调整质量直至满足大小要求（例如100KB）
    while (outputStream.toByteArray().size > 100 * 1024 && quality > 10) {
        outputStream.reset()
        quality -= 10
        bitmap.compress(format, quality, outputStream)
    }
    return outputStream.toByteArray()
}

fun compressImageStream(inputStream: InputStream, targetWidth: Int, targetHeight: Int): ByteArray {
    // 1. 尺寸压缩
    val bitmap = decodeSampledBitmap(inputStream, targetWidth, targetHeight)
        ?: throw Exception("Image decode failure")
    inputStream.close() // 及时关闭流

    // 2. 质量压缩（优先使用WebP）
    val compressedData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        compressBitmap(bitmap, Bitmap.CompressFormat.WEBP_LOSSY)
    } else {
        compressBitmap(bitmap, Bitmap.CompressFormat.WEBP)
    }
    bitmap.recycle() // 释放Bitmap内存

    return compressedData
}
