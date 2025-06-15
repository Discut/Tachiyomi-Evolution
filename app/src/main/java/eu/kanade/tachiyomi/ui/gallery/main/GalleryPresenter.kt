package eu.kanade.tachiyomi.ui.gallery.main

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.data.gallery.request.MergeImageRequest
import eu.kanade.tachiyomi.data.gallery.request.SplitImageRequest
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.model.UnionImageBO
import eu.kanade.tachiyomi.model.getDateTimeTag
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo
import eu.kanade.tachiyomi.ui.reader.sheet.toTagVo
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.random.Random

class GalleryPresenter(
    val db: DatabaseHelper = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val galleryManager: GalleryManager = Injekt.get(),
    val targetTag: TagBo? = null,
) : BaseCoroutinePresenter<GalleryController>() {

    val preference: PreferencesHelper by injectLazy()

    val sourceImage: Flow<List<IImageBo>> by lazy {
        /*(
            targetTag?.let { galleryManager.getAllImagesByTagIdAsFlow(it.tagId) }
                ?: galleryManager.sourceImage
            ).map {
            it.asSequence().sortedByDescending { it.cachedTimeMillis }.toList()
        }*/

        selectedTags.flatMapLatest { selectedTags ->
            galleryManager.getAllImagesByTagIdsAsFlow(selectedTags.map { it.tagId })
        }.map {
            it.asSequence().sortedByDescending { it.cachedTimeMillis }.toList()
        }
    }

    var sourceImageSortDate: MutableStateFlow<Map<String, List<IImageBo>>> =
        MutableStateFlow(
            emptyMap(),
        )

    val selectedTags = MutableStateFlow(targetTag?.let { listOf(it.toTagVo(true)) } ?: emptyList())

    val tagsFlow: Flow<List<TagVo>>
        get() {
            return galleryManager.tagsFlow.combine(selectedTags) { tags, selectedTags ->
                tags.map {
                    it.toTagVo(
                        selectedTags.any { selectedTag -> selectedTag.tagId == it.tagId },
                    )
                }
                    .sortedBy { !it.isSelected }
            }
        }

    override fun onCreate() {
        super.onCreate()

        presenterScope.launchIO {
            sourceImage.collect { it ->
                sourceImageSortDate.value = it
                    .groupBy { it.getDateTimeTag() }
                    .toSortedMap(compareByDescending { it }) // 按日期降序排序
            }
        }
    }

    fun randomImageList(images: List<IImageBo>): List<IImageBo> {
        return images.shuffled(Random(System.nanoTime()))
    }

    fun clickTag(tag: TagVo) {
        val tagVos = selectedTags.value
        if (tag.isSelected) {
            selectedTags.value =
                tagVos.filter { it.tagId != tag.tagId }
        } else {
            selectedTags.value =
                (tagVos + tag).distinctBy { it.tagId }
        }
    }

    fun hideImages(images: List<IImageBo>) {
        if (images.isEmpty()) {
            return
        }
        presenterScope.launchIO {
            galleryManager.changeImagesVisibility(images, false)
        }
    }

    fun showImages(images: List<IImageBo>) {
        if (images.isEmpty()) {
            return
        }
        presenterScope.launchIO {
            galleryManager.changeImagesVisibility(images, true)
        }
    }

    fun deleteImages(images: List<IImageBo>) {
        if (images.isEmpty()) {
            return
        }
        presenterScope.launchIO {
            galleryManager.deleteImages(images)
        }
    }

    fun mergeImages(
        images: List<IImageBo>,
        title: String = "合并图集",
        header: ImageBO? = null,
    ) {
        if (images.isEmpty()) {
            return
        }
        val realHeader = header ?: images.filterIsInstance<ImageBO>()[0]

        presenterScope.launchIO {
            galleryManager.mergeImages(
                MergeImageRequest(
                    diffGroupName = title,
                    target = images.filterIsInstance<UnionImageBO>().firstOrNull(),
                    images = images.filterIsInstance<ImageBO>(),
                    headerImage = realHeader,
                ),
            )
        }
    }

    fun splitDiffGroup(targets: List<UnionImageBO>) {
        if (targets.isEmpty()) {
            return
        }
        presenterScope.launchIO {
            galleryManager.splitDiffGroup(
                SplitImageRequest(
                    targets = targets,
                ),
            )
        }
    }
}
