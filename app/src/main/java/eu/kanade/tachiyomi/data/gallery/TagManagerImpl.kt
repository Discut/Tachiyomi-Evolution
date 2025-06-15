package eu.kanade.tachiyomi.data.gallery

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.orm.GalleryDatabase
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.model.toDBTag
import eu.kanade.tachiyomi.model.toImageBO
import eu.kanade.tachiyomi.model.toTagBo
import eu.kanade.tachiyomi.ui.gallery.tags.TagVo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.injectLazy

internal class TagManagerImpl : ITagManager {
    val db by injectLazy<DatabaseHelper>()
    val room by injectLazy<GalleryDatabase>()

    /**
     * 删除标签
     */
    override suspend fun deleteTag(tag: TagBo) {
        room.getTagDao().delete(tag.toDBTag())
    }

    /**
     * 根据id获取标签
     */
    override suspend fun getTagById(tagId: Long): TagBo? =
        room.getTagDao().getTagById(tagId)?.toTagBo()

    /**
     * 根据id获取图集
     */
    override fun getAllTagsByImageId(imageId: Long): Flow<List<TagBo>> {
        return room.getImageDao().getImageWithTagsAsFlow(imageId).map { it ->
            it?.tags?.map { it.toTagBo() } ?: emptyList()
        }
    }

    override fun getAllTagsAsFlow(): Flow<List<TagBo>> {
        return room.getTagDao().getAllAsFlow().map { it -> it.map { it.toTagBo() } }
    }

    override fun getAllTagsVoAsFlow(): Flow<List<TagVo>> {
        return room.getTagDao().getTagsWithImagesAsFlow().map { date ->
            date.map {
                TagVo(
                    tagId = it.tag.tagId,
                    name = it.tag.tagValue,
                    images = if (it.images.isNullOrEmpty()) emptyList() else it.images.map { it.toImageBO() },
                )
            }
        }
    }
}
