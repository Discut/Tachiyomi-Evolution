package eu.kanade.tachiyomi.data.gallery

import android.content.Context
import androidx.room.withTransaction
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.gallery.request.MergeImageRequest
import eu.kanade.tachiyomi.data.gallery.request.SplitImageRequest
import eu.kanade.tachiyomi.data.orm.GalleryDatabase
import eu.kanade.tachiyomi.data.orm.models.DBDiffGroup
import eu.kanade.tachiyomi.data.orm.models.DBDiffGroupImage
import eu.kanade.tachiyomi.data.orm.models.DBImage
import eu.kanade.tachiyomi.data.orm.models.DBImageAndTag
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.model.GalleryBo
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.model.UnionImageBO
import eu.kanade.tachiyomi.model.toDBTag
import eu.kanade.tachiyomi.model.toImageBO
import eu.kanade.tachiyomi.model.toTagBo
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.model.toDBImage
import eu.kanade.tachiyomi.source.gallery.model.toSImage
import eu.kanade.tachiyomi.util.plus
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import kotlin.random.Random

/**
 * 图集管理器
 */
class GalleryManager(
    val context: Context,
) : ITagManager by TagManagerImpl(),
    IBackupManager by BackupManagerImpl(),
    IThumbnailCreator by ThumbnailCreatorImpl() {
    val db by injectLazy<DatabaseHelper>()
    val room by injectLazy<GalleryDatabase>()
    val cache by injectLazy<ImageCache>()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sourceManager by injectLazy<SourceManager>()

    var sourceImage: MutableStateFlow<List<IImageBo>> = MutableStateFlow(emptyList())

    val preference: PreferencesHelper by injectLazy()

    private val galleryMap = mutableMapOf<Long, List<IImageBo>>()

    val tagsFlow by lazy {
        room.getTagDao().getAllAsFlow().map {
            it.map { it.toTagBo() }
        }
    }

    init {
        scope.launchIO {

            preference.isMergeDiffImage().asFlow().flatMapLatest { isMergeDiffImage ->
                if (isMergeDiffImage) {
                    combine(
                        room.getImageDao().getAllAsFlow(),
                        room.getDiffGroupDao().getAllDiffImagesAsFlow(),
                    ) { images, diffImage ->
                        mergeWithUnassigned(
                            images.filterPath(preference),
                            diffImage,
                        ).flatMap { it ->
                            if (it.key == "") {
                                it.value
                            } else {
                                listOf(
                                    UnionImageBO(
                                        diffGroupId = it.key,
                                        unions = it.value,
                                    ),
                                )
                            }
                        }
                    }
                } else {
                    room.getImageDao().getAllAsFlow()
                        .map { it.filterPath(preference).map { it.toImageBO() } }
                }
            }.collectLatest {
                sourceImage.value = it.sortedBy {
                    it.modifiedAt
                }
            }

            // 初始化db数据
            /*            room.getImageDao().getAllAsFlow()
                            .combine(preference.isMergeDiffImage().asFlow()) { images, isMergeDiffImage ->
                                if (isMergeDiffImage) {
                                    room.getDiffGroupDao().a
                                } else images
                            }.map {
                                it.filterPath(preference)
                                    .map { it.toImageBO(*/
            /*cache.isExist(it.id.toString())*/
            /*) }
                            }.collectLatest {
                                sourceImage.value = it.sortedBy {
                                    it.dbImage.modifiedAt
                                }
                            }*/
        }
    }

    fun putTempGallery(images: List<IImageBo>, galleryId: Long = Random.nextLong()): Long {
        galleryMap[galleryId] = images

        return galleryId
    }

    fun getGallery(galleryId: Long): List<IImageBo> {
        return galleryMap[galleryId] ?: emptyList()
    }

    suspend fun getImageBo(imageId: Long): IImageBo? = withIOContext {
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
        val dbPaths = room.getImageDao().getAll().map { it.filePath }
        val remoteImages = deferredResults.awaitAll().flatten().distinctBy { it.id }.filter {
            it.filePath !in dbPaths
        }

        room.getImageDao().insertOrUpdateAll(*remoteImages.toTypedArray())

        // 优先使用远程数据，本地数据作兜底
        val localImages = withContext(Dispatchers.Default) {
            room.getImageDao().getAll()
                .filterPath(preference)
                .map { it.toImageBO(cache.isExist(it.id.toString())) }
                .takeIf { it.isNotEmpty() }
                ?: emptyList()
        }

        // executeThumbnailJob(sortedImages)

        // 分批次更新 UI 状态（防内存溢出）
        /*sortedImages.chunked(50).forEach { chunk ->
            withContext(Dispatchers.Main) {
                sourceImage.value = chunk
            }
        }*/
        // sourceImage.value = sortedImages
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
    fun getAllImagesByTagIdsAsFlow(tagIds: List<Long>): Flow<List<IImageBo>> {
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

    /**
     * 改变图片可见性
     */
    suspend fun changeImagesVisibility(images: List<IImageBo>, visible: Boolean) {
        room.withTransaction {
            images.flatMap {
                when (it) {
                    is ImageBO -> listOf(it)
                    is UnionImageBO -> it.unions
                    else -> emptyList()
                }
            }.map {
                it.dbImage.copy(
                    isHide = !visible,
                )
            }.forEach {
                room.getImageDao().update(it)
            }
        }
    }

    suspend fun deleteImages(images: List<IImageBo>) {
        if (images.isEmpty()) {
            return
        }
        val images = images.flatMap {
            when (it) {
                is ImageBO -> listOf(it)
                is UnionImageBO -> it.unions
                else -> emptyList()
            }
        }
        room.withTransaction {
            images.forEach {
                room.getImageAndTagDao().deleteByImages(it.id)
                room.getImageDao().delete(it.dbImage)
            }
        }

        images.groupBy { it.source }.forEach { (source, imageBOS) ->
            sourceManager.getGallerySource(source)?.let {
                it.deleteImage(imageBOS.map { bo -> bo.dbImage.toSImage() }.toList())
            }
        }
    }

    suspend fun mergeImages(request: MergeImageRequest) {
        if (request.images.isEmpty()) {
            return
        }
        val diffGroupDao = room.getDiffGroupDao()

        room.withTransaction {
            val (diffGroup, merged) = if (request.target == null) {
                val diffGroup = DBDiffGroup(
                    groupId = UUID.randomUUID().toString(),
                    groupName = request.diffGroupName.ifEmpty { "合并图集" },
                    createdAt = request.createDate,
                )
                diffGroupDao.insert(diffGroup)
                diffGroup to emptyList<Long>()
            } else {
                val dbDiffGroupImages = diffGroupDao.getByImageId(request.target.unions.first().id)
                if (dbDiffGroupImages.isEmpty()) {
                    return@withTransaction
                }
                val group = diffGroupDao.getGroupById(dbDiffGroupImages.first().groupId)
                    ?: return@withTransaction
                group to request.target.unions.map { it.id }
            }

            val groupImages = diffGroupDao.getByGroupId(diffGroup.groupId)
            val sortOrder = if (groupImages.isEmpty()) {
                BigDecimal(1000)
            } else {
                groupImages.last().sortOrder
            }
            val waitMerge = request.images.filter { it.id !in merged }

            val dbDiffGroupImageList = waitMerge.mapIndexed { index, imageBO ->
                DBDiffGroupImage(
                    groupId = diffGroup.groupId,
                    imageId = imageBO.id,
                    sortOrder = sortOrder + (index + 1) * 1000,
                )
            }
            diffGroupDao.insertAllImages(*dbDiffGroupImageList.toTypedArray())
        }
    }

    suspend fun splitDiffGroup(request: SplitImageRequest) {
        val diffGroupDao = room.getDiffGroupDao()
        if (request.targets.isEmpty()) {
            return
        }

        room.withTransaction {
            request.targets.forEach {
                if (it.unions.isEmpty()) {
                    return@forEach
                }
                val dbDiffGroupImages = diffGroupDao.getByImageId(
                    it.unions.first().id,
                )
                if (dbDiffGroupImages.isEmpty()) {
                    return@forEach
                }

                val group =
                    diffGroupDao.getGroupById(dbDiffGroupImages.first().groupId) ?: return@forEach
                diffGroupDao.delete(group)
            }
        }
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

    fun mergeWithUnassigned(
        images: List<DBImage>,
        relations: List<DBDiffGroupImage>,
    ): Map<String, List<ImageBO>> {
        // 构建快速查找关系（O(n)时间复杂度）
        val imageMap = images.associateBy { it.id } // 网页2的映射优化

        // 创建反向索引加速查找（O(m)预处理）
        val imageHasGroup = relations.map { it.imageId }.toHashSet()

        // 使用分组合并+默认组处理
        return (relations + generateUnassigned(images, imageHasGroup))
            .groupingBy { it.groupId }
            .fold<DBDiffGroupImage, String, MutableList<ImageBO>>(

                initialValueSelector = { _, _ -> mutableListOf() },
                operation = { _, acc, relation ->
                    imageMap[relation.imageId]?.let { acc.add(it.toImageBO(order = relation.sortOrder)) }
                    acc
                },

            ).map {
                it.key to it.value.sortedBy { it.order }
            }.toMap()
    }

    // 生成未关联的虚拟关系对象
    private fun generateUnassigned(
        images: List<DBImage>,
        existingIds: Set<Long>,
    ): List<DBDiffGroupImage> {
        return images
            .asSequence()
            .filter { it.id !in existingIds }
            .map { DBDiffGroupImage(groupId = "", imageId = it.id, sortOrder = BigDecimal.ZERO) }
            .toList()
    }
}
