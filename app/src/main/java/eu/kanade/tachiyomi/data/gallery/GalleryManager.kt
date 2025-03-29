package eu.kanade.tachiyomi.data.gallery

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.DBImage
import eu.kanade.tachiyomi.model.GalleryBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.model.toDBImage
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class GalleryManager(
    val context: Context,
) {
    val db by injectLazy<DatabaseHelper>()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sourceManager by injectLazy<SourceManager>()

    var sourceImage: MutableStateFlow<List<ImageBO>> = MutableStateFlow(emptyList())

    private val galleryMap = mutableMapOf<Long, List<ImageBO>>()

    fun putTempGallery(images: List<ImageBO>, galleryId: Long = Random.nextLong()): Long {
        galleryMap[galleryId] = images

        return galleryId
    }

    fun getGallery(galleryId: Long): List<ImageBO> {
        return galleryMap[galleryId] ?: emptyList()
    }

    fun buildGallery(galleryId: Long): GalleryBo {
        val imageBOS = getGallery(galleryId)
        if (imageBOS.isEmpty()) {
            return GalleryBo.EMPTY
        }

        return GalleryBo().apply {
            images = imageBOS
            id = galleryId
        }
    }

    /*    fun syncImages() {
            scope.launchIO {
                val sources = sourceManager.getAllGallerySources()
                if (sources.isEmpty()) {
                    return@launchIO
                }
                sources.forEach {
                    async {

                    }
                }


                var localImages = db.getAllImages().executeAsBlocking().map { ImageBO(dbImage = it) }

                if (localImages.isEmpty()) {
                    val allImages = localSource?.getAllImages()
                    allImages?.map { it.toDBImage() }?.let {
                        db.insertImages(it).executeAsBlocking()
                    }
                    localImages = allImages?.map { ImageBO(sImage = it) } ?: emptyList()
                }

                val allImages = localImages // localSource?.collectImages(1, 50) ?: Page<SImage>(0, 0, 0, emptyList())
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                sourceImage.value = allImages
                    .sortedBy {
                        try {
                            LocalDateTime.parse(it.createdTime, formatter)
                        } catch (e: Exception) {
                            LocalDateTime.MIN
                        }
                    }
            }
            return sourceImage
        }*/

    fun syncImages() = scope.launchIO {
        // 初始化db数据
        pushDbData()

        val sources = sourceManager.getAllGallerySources()
        if (sources.isEmpty()) return@launchIO

        // 并行获取所有源的图片数据
        val deferredResults = sources.map { source ->
            async {
                try {
                    source.getAllImages().map { it.toDBImage() }
                } catch (e: Exception) {
                    emptyList<DBImage>().also {
                        Timber.e("Source ${source.id} failed.")
                        Timber.e(e)
                    }
                }
            }
        }

        // 合并并插入数据库（去重处理）
        val remoteImages = deferredResults.awaitAll().flatten().distinctBy { it.id }
        db.insertImages(remoteImages).executeAsBlocking()

        // 优先使用远程数据，本地数据作兜底
        val localImages = withContext(Dispatchers.Default) {
            db.getAllImages().executeAsBlocking()
                .map { ImageBO(dbImage = it) }
                .takeIf { it.isNotEmpty() }
                ?: emptyList()
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        // 使用预计算的时间戳优化排序
        val sortedImages = localImages.sortedBy {
            try {
                LocalDateTime.parse(it.createdTime, formatter)
            } catch (e: Exception) {
                LocalDateTime.MIN
            }
        }

        // 分批次更新 UI 状态（防内存溢出）
        /*sortedImages.chunked(50).forEach { chunk ->
            withContext(Dispatchers.Main) {
                sourceImage.value = chunk
            }
        }*/
        sourceImage.value = sortedImages
    }

    private fun pushDbData() = scope.launchIO {
        val localImages = db.getAllImages().executeAsBlocking().map { ImageBO(dbImage = it) }

        val allImages = localImages // localSource?.collectImages(1, 50) ?: Page<SImage>(0, 0, 0, emptyList())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        sourceImage.value = allImages
            .sortedBy {
                try {
                    LocalDateTime.parse(it.createdTime, formatter)
                } catch (e: Exception) {
                    LocalDateTime.MIN
                }
            }
    }
}
