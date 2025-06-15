package eu.kanade.tachiyomi.data.gallery

import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.util.getImageStream
import eu.kanade.tachiyomi.util.toSImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

internal class ThumbnailCreatorImpl : IThumbnailCreator {

    private val cache: ImageCache by injectLazy()

    private val existenceCache = ConcurrentHashMap<String, Boolean>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var job: Job? = null

    private val waitList = Collections.synchronizedList(mutableListOf<IImageBo>())

    private val progressingList = Collections.synchronizedList(mutableListOf<String>())

    override fun createThumbnail(image: IImageBo) {
        pushImageToQueue(image)
    }

    override suspend fun getThumbnail(image: IImageBo): InputStream {
        if (existsThumbnail(image)) {
            return cache.getAsStream(image.id.toString())
        } else {
            pushImageToQueue(image)
            val sImage = image.toSImage()
            return sImage.getImageStream()?.originStream ?: throw Exception("Image not found")
        }
    }

    override fun existsThumbnail(image: IImageBo): Boolean {
        val key = image.id.toString()
        // 1. 检查内存缓存
        existenceCache[key]?.let { return it }

        // 2. 检查磁盘缓存（避免重复磁盘访问）
        val exists = cache.isExist(key)
        existenceCache[key] = exists // 缓存结果
        return exists
    }

    override fun deleteThumbnail(image: IImageBo) {
        cache.delete(image.id.toString())
    }

    override fun boot() {
        if (job?.isActive == true) {
            return
        }
        job = progress()
        job?.invokeOnCompletion {
            synchronized(waitList) {
                if (waitList.size == 0) {
                    return@synchronized
                }
                job = progress()
            }
        }
    }

    private fun progress() =
        scope.launch {
            val imageSource = waitList.removeAllAndReturn()
            progressingList.addAll(imageSource.map { it.id.toString() })
            processImageBatches(imageSource, 5)
                .catch { e ->
                    Timber.e(e)
                }
                .collectLatest { result ->
                    when (result) {
                        is ProcessResult.BatchComplete -> {
                            Timber.i("完成批次${result.batchId}, 大小 ${result.results.size}， 包含 ${result.results.filterIsInstance<ProcessedResult.Success>().size}个成功， 内容 \n${result.results}")
                            progressingList.removeAll(
                                result.results.filterIsInstance<ProcessedResult.Success>()
                                    .map { it.imageId },
                            )
                            /*showToast()
                            updateProgressBar()*/
                        }

                        is ProcessResult.Error -> {
                            Timber.e("批次${result.batchId}出错")
                            Timber.e(result.exception)
                        }
                    }
                }
        }

    private fun pushImageToQueue(image: IImageBo) {
        if (progressingList.contains(image.id.toString())) {
            Timber.d("图片 ${image.id} 正在处理中")
        } else {
            waitList.add(image)
        }
        boot()
    }

    private fun <T> MutableList<T>.removeAllAndReturn(): List<T> {
        return synchronized(this) {
            val removedItems = this.toList()
            this.clear()
            removedItems
        }
    }
}
