package eu.kanade.tachiyomi.ui.reader.sheet

import android.app.Activity
import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.data.orm.models.DBTagType.PresetType
import eu.kanade.tachiyomi.data.tagger.WDTagger
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.util.generateTimestampBasedID
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import uy.kohesive.injekt.injectLazy

class TagSettingsViewModel(
    private val activity: Activity,
    private val imageBO: IImageBo,
) {
    val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val galleryManager by injectLazy<GalleryManager>()
    private val wdTagger by injectLazy<WDTagger>()

    private val searchKeyFlow = MutableStateFlow("")

    val resultTagsFlow = galleryManager.tagsFlow.combine(
        galleryManager.getAllTagsByImageId(imageBO.id),
    ) { left, right -> Pair(left, right) }
        .combine(searchKeyFlow) { (left, mid), right ->
            filterTag(right, left, mid)
        }

    // AI 打标状态
    val aiPredictState = MutableStateFlow(AIPredictState.IDLE)
    val aiPredictResults = MutableStateFlow<List<AIPredictResult>>(emptyList())

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

    /**
     * 触发 AI 打标推理
     */
    suspend fun predictTags(filePath: String) {
        aiPredictState.value = AIPredictState.LOADING
        try {
            // 获取当前图片已关联的标签名集合（用于默认采纳判断）
            val existingTagNames = galleryManager.getAllTagsByImageId(imageBO.id)
                .first()
                .map { it.tagValue.lowercase() }
                .toSet()

            val bitmap = BitmapFactory.decodeFile(filePath)
                ?: throw IllegalStateException("无法解码图片文件: $filePath")
            val predictions = wdTagger.predictAsync(bitmap)
            bitmap.recycle()

            if (predictions.isEmpty()) {
                aiPredictResults.value = emptyList()
                aiPredictState.value = AIPredictState.EMPTY
            } else {
                aiPredictResults.value = predictions.map { (name, score) ->
                    AIPredictResult(
                        tagName = name,
                        score = score,
                        isAdopted = name.lowercase() in existingTagNames,
                    )
                }
                aiPredictState.value = AIPredictState.RESULTS
            }
        } catch (e: Exception) {
            aiPredictState.value = AIPredictState.ERROR
            throw e
        }
    }

    /**
     * 采纳 AI 标签：创建标签(type_id=4)并关联到当前图片
     */
    suspend fun adoptAITag(result: AIPredictResult) {
        if (imageBO !is ImageBO) return
        if (result.isAdopted) return

        val newTag = TagBo(
            tagId = generateTimestampBasedID(),
            tagValue = result.tagName,
            typeId = PresetType.AUTO.id.toLong(), // auto
            source = 0,
        )

        // 如果标签名已存在则跳过创建，使用已存在的标签
        val existingTag = galleryManager.getAllTags()
            .find { it.tagValue.equals(result.tagName, ignoreCase = true) }

        val tag = if (existingTag != null) {
            existingTag
        } else {
            galleryManager.createTag(newTag)
            newTag
        }

        galleryManager.relatedImageAndTag(imageBO, tag)

        // 标记结果已采纳
        val currentResults = aiPredictResults.value.toMutableList()
        val index = currentResults.indexOf(result)
        if (index >= 0) {
            currentResults[index] = result.copy(isAdopted = true)
            aiPredictResults.value = currentResults
        }
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
