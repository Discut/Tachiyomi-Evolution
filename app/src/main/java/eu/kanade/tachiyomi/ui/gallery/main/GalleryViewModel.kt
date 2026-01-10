package eu.kanade.tachiyomi.ui.gallery.main

import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.R
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
import eu.kanade.tachiyomi.ui.base.viewmodel.BaseViewModel
import eu.kanade.tachiyomi.ui.gallery.ViewUtil
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryEffect
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryEvent
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryItem
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryMainState
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo
import eu.kanade.tachiyomi.ui.reader.sheet.toTagVo
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.abs
import kotlin.random.Random

class GalleryViewModel(
    val db: DatabaseHelper = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val galleryManager: GalleryManager = Injekt.get(),
    val targetTag: TagBo? = null,
) : BaseViewModel<GalleryMainState, GalleryEvent, GalleryEffect>() {

    val preference: PreferencesHelper by injectLazy()

    private val appbarTitle = App.instance?.getString(R.string.gallery) ?: "Gallery"
    private var curWidth: Int = 0
    private var lastWidth = 0
    private var curHeight = 0

    private var rowImageMaxSize = preference.getRowImageMaxSize().get()

    private var calculateJob: Job? = null

    private val isReady: Boolean
        get() {
            return curWidth > 0 && curHeight > 0
        }

    val sourceImage: Flow<List<IImageBo>> by lazy {
        selectedTags.flatMapLatest { selectedTags ->
            galleryManager.getAllImagesByTagIdsAsFlow(selectedTags.map { it.tagId })
        }.map {
            it.asSequence().sortedByDescending { it.cachedTimeMillis }.toList()
        }
    }

    private var sourceImageSortDate: Map<String, List<IImageBo>> = emptyMap()

    val selectedTags = MutableStateFlow(targetTag?.let { listOf(it.toTagVo(true)) } ?: emptyList())

    private val tagsFlow: Flow<List<TagVo>>
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

    override fun initialState(): GalleryMainState = GalleryMainState.Loading

    override suspend fun handleEvent(
        event: GalleryEvent,
        state: GalleryMainState,
    ): GalleryMainState {
        return when (event) {
            is GalleryEvent.Load -> {
                if (handleWidthChange(event.containerWidth, event.containerHeight)) {
                    calculateColumnCount(sourceImageSortDate)
                    state.transformToLoadingState()
                } else {
                    state
                }
            }

            is GalleryEvent.SelectTag -> {
                clickTag(event.tag)
                state.transformToLoadingState()
            }

            is GalleryEvent.ClearAllSelectedTags -> {
                selectedTags.value = emptyList()
                state.transformToLoadingState()
            }

            GalleryEvent.RandomPlay -> {
                val imageBos = sourceImage.last()
                if (imageBos.isEmpty()) {
                    sendEffect {
                        GalleryEffect.ShowToast("没有图片")
                    }
                    return state
                }

                sendEffect {
                    GalleryEffect.LaunchReader(randomImageList(imageBos), 0)
                }
                state
            }
        }
    }

    private fun GalleryMainState.transformToLoadingState(): GalleryMainState {
        return when (this) {
            is GalleryMainState.Error -> GalleryMainState.Loading
            is GalleryMainState.Success -> this.copy(isLoading = true)
            else -> this
        }
    }

    private fun calculateColumnCount(dateImages: Map<String, List<IImageBo>>) {
        calculateJob?.cancel()
        calculateJob = presenterScope.launch(Dispatchers.Default) {
            val innerList: MutableList<GalleryItem> = mutableListOf()
            val itemMap = if (preference.isShowHideImages().get()) {
                dateImages
            } else {
                dateImages.map {
                    it.key to it.value.filter { !it.isHide }
                }.toMap()
            }
            itemMap.forEach { pair ->
                val (title, images) = pair
                innerList.add(GalleryItem.Header(title, images))
                innerList.addAll(
                    ViewUtil.calculateImageRowV2(
                        minHeight = curHeight / 7,
                        maxHeight = (curHeight / 2),
                        screenWidth = curWidth - 16.dpToPx,
                        spacing = 4.dpToPx,
                        maxSize = rowImageMaxSize,
                        images = images,
                        acc = emptyList(),
                    ).map {
                        it.text = title
                        it
                    },
                )
            }
            val selectedTags = selectedTags.value
            val tagVos = galleryManager.getAllTags().map {
                it.toTagVo(
                    selectedTags.any { selectedTag -> selectedTag.tagId == it.tagId },
                )
            }.sortedBy { !it.isSelected }

            innerList.add(
                0,
                GalleryItem.AppBar(
                    text = if (selectedTags.isEmpty()) {
                        appbarTitle
                    } else {
                        "\" ${selectedTags.joinToString { it.tagValue }} \""
                    },
                    tags = tagVos,
                ),
            )

            sendState {
                val state = state.value
                if (state is GalleryMainState.Success) {
                    state.copy(
                        isLoading = false,
                        items = innerList,
                        tags = tagVos,
                    )
                } else {
                    GalleryMainState.Success(
                        isLoading = false,
                        items = innerList,
                        tags = tagVos,
                    )
                }
            }
        }
    }

    private fun handleWidthChange(containerWidth: Int, containerHeight: Int): Boolean {
        if (containerWidth <= 0 || containerHeight <= 0) {
            return false
        }
        curHeight = containerHeight
        if (abs(containerWidth - lastWidth) > 5) {
            lastWidth = curWidth
            curWidth = containerWidth
            return true
        } else {
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()

        presenterScope.launch {
            preference.getRowImageMaxSize().asFlow().collectLatest {
                rowImageMaxSize = it
            }
        }
        presenterScope.launchIO {
            sourceImage.collect { it ->
                sourceImageSortDate = it
                    .groupBy { it.getDateTimeTag() }
                    .toSortedMap(compareByDescending { it }) // 按日期降序排序
                if (isReady) {
                    calculateColumnCount(sourceImageSortDate)
                }
            }
        }
    }

    fun randomImageList(images: List<IImageBo>): List<IImageBo> {
        return images.shuffled(Random(System.nanoTime()))
    }

    private fun clickTag(tag: TagVo) {
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
