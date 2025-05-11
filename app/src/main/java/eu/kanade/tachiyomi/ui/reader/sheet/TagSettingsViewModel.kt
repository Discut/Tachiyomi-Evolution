package eu.kanade.tachiyomi.ui.reader.sheet

import android.app.Activity
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import uy.kohesive.injekt.injectLazy

class TagSettingsViewModel(
    private val activity: Activity,
    private val imageBO: IImageBo,
) {
    val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val galleryManager by injectLazy<GalleryManager>()

    private val searchKeyFlow = MutableStateFlow("")

    val resultTagsFlow = galleryManager.tagsFlow.combine(
        galleryManager.getAllTagsByImageId(imageBO.id),
    ) { left, right -> Pair(left, right) }
        .combine(searchKeyFlow) { (left, mid), right ->
            filterTag(right, left, mid)
        }

    companion object {
        val PLUS_TAG = TagVo(
            tagId = 0,
            tagValue = "+",
            isSelected = false,
            typeId = -1,
            source = -1,
        )
    }

    var searchKey: String
        get() = searchKeyFlow.value
        set(value) {
            searchKeyFlow.value = value
        }

    suspend fun createNewTag(tag: TagVo) {
        galleryManager.createTag(tag.toTagBo())
    }

    suspend fun relatedImageAndTag(image: ImageBO, tag: TagVo) {
        galleryManager.relatedImageAndTag(image, tag.toTagBo())
    }

    suspend fun unrelatedImageAndTag(image: ImageBO, tag: TagVo) {
        galleryManager.unrelatedImageAndTag(image, tag.toTagBo())
    }

    suspend fun deleteTag(tag: TagVo) = withIOContext {
        galleryManager.deleteTag(tag.toTagBo())
    }

    private suspend fun filterTag(
        query: String,
        tags: List<TagBo>,
        selectedTags: List<TagBo>,
    ): List<TagVo> {
        val filteredTags = tags
            .asSequence()
            .filter { it.tagValue.contains(query, true) }
            .toMutableList()
        val mergedTags = (selectedTags + filteredTags)
            .distinctBy { it.tagId }
            .mapTo(mutableListOf()) { tag ->
                val isSelected = selectedTags.any { it.tagId == tag.tagId }
                tag.toTagVo(isSelected)
            }
        mergedTags.sortBy { !it.isSelected }
        mergedTags.add(PLUS_TAG)
        return mergedTags.toList()
    }
}
