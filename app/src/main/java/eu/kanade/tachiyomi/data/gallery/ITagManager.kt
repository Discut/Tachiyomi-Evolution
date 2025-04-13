package eu.kanade.tachiyomi.data.gallery

import eu.kanade.tachiyomi.model.TagBo
import kotlinx.coroutines.flow.Flow

interface ITagManager {
    suspend fun getTagById(tagId: Long): TagBo?

    /**
     * 删除标签
     */
    suspend fun deleteTag(tag: TagBo)

    /**
     * 根据id获取图集
     */
    fun getAllTagsByImageId(imageId: Long): Flow<List<TagBo>>
}
