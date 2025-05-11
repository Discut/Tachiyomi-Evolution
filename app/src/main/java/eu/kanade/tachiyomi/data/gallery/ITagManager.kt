package eu.kanade.tachiyomi.data.gallery

import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.ui.gallery.tags.TagVo
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

    /**
     * 获取所有标签
     */
    fun getAllTagsAsFlow(): Flow<List<TagBo>>

    /**
     * 获取所有标签携带图片
     */
    fun getAllTagsVoAsFlow(): Flow<List<TagVo>>
}
