package eu.kanade.tachiyomi.data.gallery

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database_orm.GalleryDatabase
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.model.toDBTag
import eu.kanade.tachiyomi.model.toTagBo
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
}
