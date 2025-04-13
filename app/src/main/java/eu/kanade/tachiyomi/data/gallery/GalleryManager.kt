package eu.kanade.tachiyomi.data.gallery

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database_orm.GalleryDatabase
import eu.kanade.tachiyomi.data.database_orm.models.DBImage
import eu.kanade.tachiyomi.data.database_orm.models.DBImageAndTag
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.model.GalleryBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.model.toDBTag
import eu.kanade.tachiyomi.model.toImageBO
import eu.kanade.tachiyomi.model.toTagBo
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.model.toDBImage
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.random.Random

/**
 * 图集管理器
 */
class GalleryManager(
    val context: Context,
) : ITagManager by TagManagerImpl(),
    IBackupManager by BackupManagerImpl() {
    val db by injectLazy<DatabaseHelper>()
    val room by injectLazy<GalleryDatabase>()
    val cache by injectLazy<ImageCache>()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sourceManager by injectLazy<SourceManager>()

    var sourceImage: MutableStateFlow<List<ImageBO>> = MutableStateFlow(emptyList())

    val preference: PreferencesHelper by injectLazy()

    private val galleryMap = mutableMapOf<Long, List<ImageBO>>()

    val tagsFlow by lazy {
        room.getTagDao().getAllAsFlow().map {
            it.map { it.toTagBo() }
        }
    }

    fun putTempGallery(images: List<ImageBO>, galleryId: Long = Random.nextLong()): Long {
        galleryMap[galleryId] = images

        return galleryId
    }

    fun getGallery(galleryId: Long): List<ImageBO> {
        return galleryMap[galleryId] ?: emptyList()
    }

    suspend fun getImageBo(imageId: Long): ImageBO? = withIOContext {
        val dbImage = room.getImageDao().getById(imageId) ?: return@withIOContext null
        return@withIOContext dbImage.toImageBO(cache.isExist(dbImage.id.toString()))
    }

    /**
     * 根据id获取图集
     */
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

    /**
     * 同步图集
     */
    fun syncImages() = scope.launchIO {
        Timber.i("开始同步图集")
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

        room.getImageDao().insertOrUpdateAll(*remoteImages.toTypedArray())

        // 优先使用远程数据，本地数据作兜底
        val localImages = withContext(Dispatchers.Default) {
            room.getImageDao().getAll()
                .filterPath(preference)
                .map { it.toImageBO(cache.isExist(it.id.toString())) }
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

        // executeThumbnailJob(sortedImages)

        // 分批次更新 UI 状态（防内存溢出）
        /*sortedImages.chunked(50).forEach { chunk ->
            withContext(Dispatchers.Main) {
                sourceImage.value = chunk
            }
        }*/
        sourceImage.value = sortedImages
    }

    /**
     * 创建标签
     */
    suspend fun createTag(tag: TagBo) {
        room.getTagDao().insert(tag.toDBTag())
    }

    /**
     * 关联图集和标签
     */
    suspend fun relatedImageAndTag(image: ImageBO, tag: TagBo) =
        room.getImageAndTagDao().insert(
            DBImageAndTag(
                imageId = image.id,
                tagId = tag.tagId,
            ),
        )

    /**
     * 取消关联图集和标签
     */
    suspend fun unrelatedImageAndTag(image: ImageBO, tag: TagBo) =
        room.getImageAndTagDao().delete(
            DBImageAndTag(
                imageId = image.id,
                tagId = tag.tagId,
            ),
        )

    /**
     * 根据id获取图集
     */
    fun getAllImagesByTagIdAsFlow(tagId: Long): Flow<List<ImageBO>> {
        return room.getTagDao().getTagWithImagesAsFlow(tagId).map { it ->
            it?.images?.filterPath(preference)
                ?.sortedBy {
                    it.modifiedAt.toLocalDateTime()
                }?.map { it.toImageBO(cache.isExist(it.id.toString())) } ?: emptyList()
        }
    }

    /**
     * 根据id获取图集
     */
    fun getAllImagesByTagIdsAsFlow(tagIds: List<Long>): Flow<List<ImageBO>> {
        return if (tagIds.isEmpty()) {
            sourceImage
        } else {
            room.getImageDao().getImagesWithAllTagsAsFlow(tagIds, tagIds.size).map { it ->
                it.filterPath(preference)
                    .map { image ->
                        image.toImageBO(cache.isExist(image.id.toString()))
                    }
            }
        }
    }

    /**
     * 获取图集数量
     */
    suspend fun countImages(): Long {
        return room.getImageDao().count()
    }

    // 扩展函数形式
    private fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return this.toInstant()
            .atZone(zoneId) // 转换为带时区的 ZonedDateTime
            .toLocalDateTime() // 剥离时区信息
    }

    private fun List<DBImage>.filterPath(preference: PreferencesHelper): List<DBImage> {
        // 1. 使用解构声明提升可读性
        val (excluded, included) = getValidPaths(preference)
        if (included.isEmpty()) return emptyList()

        // 2. 预处理包含路径为前缀匹配
        val includePrefixes = included.map { it.addSuffixIfNeeded() }.toSet()
        val excludeSet = excluded.map { it.addSuffixIfNeeded() }.toSet()

        // 3. 并行流加速过滤（大数据量场景）
        return this.asSequence()
            .filter { image ->
                image.filePath.isNotEmpty() &&
                    includePrefixes.any { image.filePath.startsWith(it) } &&
                    excludeSet.none { image.filePath.startsWith(it) }
            }.toList()
    }

    // 提取路径处理逻辑
    private fun getValidPaths(preference: PreferencesHelper): Pair<Set<String>, Set<String>> {
        val excluded = preference.getGalleryScanExcludes().get().toMutableSet()
        val included = preference.getGalleryScanIncludes().get()
            .filterNot(excluded::contains)
            .takeIf(List<String>::isNotEmpty) ?: return emptySet<String>() to emptySet()

        // 修正路径包含逻辑：排除路径包含包含路径时过滤
        val validIncluded = included.filterNot { include ->
            excluded.any { exclude -> include.startsWith(exclude) }
        }.toSet()

//        val validExcluded = excluded.filterNot { exclude ->
//            validIncluded.any { include -> exclude.startsWith(include) }
//        }.toSet()

        return excluded to validIncluded
    }

    // 处理路径结尾斜杠一致性
    private fun String.addSuffixIfNeeded() = if (endsWith('/')) this else "$this/"

    /**
     * 执行缩略图任务
     */
    private fun executeThumbnailJob(images: List<ImageBO>) = scope.launchIO {
        processImageBatches(images, 5)
            .onEach { result ->
                when (result) {
                    is ProcessResult.BatchComplete -> {
                        Timber.i("完成批次${result.batchId}")
                        /*showToast()
                        updateProgressBar()*/
                    }

                    is ProcessResult.Error -> {
                        Timber.e("批次${result.batchId}出错")
                        Timber.e(result.exception)
                    }
                }
            }
            .flowOn(Dispatchers.Main)
            .catch { e -> Timber.e(e) }
            .collect()
    }

    /**
     * 推送本地图集数据到缓存
     */
    private fun pushDbData() = scope.launchIO {
        val localImages =
            room.getImageDao().getAll()
                .filterPath(preference)
                .map { it.toImageBO(cache.isExist(it.id.toString())) }

        val allImages =
            localImages // localSource?.collectImages(1, 50) ?: Page<SImage>(0, 0, 0, emptyList())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        sourceImage.value = allImages
            .sortedBy {
                it.dbImage.modifiedAt
            }
    }
}
