package eu.kanade.tachiyomi.ui.gallery.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.databinding.GalleryControllerBinding
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.theme.GalleryTheme
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.gallery.ViewUtil
import eu.kanade.tachiyomi.ui.gallery.component.GalleryImageFlow
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryItem
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.toast
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import my.nanihadesuka.compose.controller.LazyListStateController
import my.nanihadesuka.compose.controller.rememberLazyListStateController
import uy.kohesive.injekt.injectLazy
import kotlin.math.abs

class GalleryController(
    bundle: Bundle? = null,
    private var targetTag: TagBo? = null,
) :
    BaseCoroutineController<GalleryControllerBinding, GalleryPresenter>(bundle),
    RootSearchInterface {

    private val galleryManager: GalleryManager by injectLazy()

    override val presenter by lazy {
        GalleryPresenter(targetTag = targetTag)
    }

    override fun createBinding(inflater: LayoutInflater): GalleryControllerBinding {
        return GalleryControllerBinding.inflate(inflater)
    }

    override fun getTitle(): String? = null

    private fun getComposeTitle() = view?.context?.getString(R.string.gallery) ?: "Gallery"
    /*if (targetTag != null) {
        "\" ${targetTag?.tagValue} \""
    } else {
        view?.context?.getString(R.string.gallery)
    } ?: "Gallery"*/

    override fun getSearchTitle(): String {
        return "search"
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        activity?.findViewById<View>(R.id.app_bar)?.visibility = View.GONE
        view.findViewById<ComposeView>(R.id.gallery_container)?.setContent {
            val isDarkTheme = when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> isSystemInDarkTheme() // You can define this function to check system theme preference if needed
            }
            val screenHeight = resources?.displayMetrics?.heightPixels

            GalleryTheme {
                ProvideTextStyle(
                    value = LocalTextStyle.current.copy(
                        color = if (isDarkTheme) Color.White else Color.Black,
                    ),
                ) {
                    val prefetchStrategy = remember {
                        LazyListPrefetchStrategy(
                            nestedPrefetchItemCount = 6,
                        )
                    }
                    val scrollState: LazyListState = rememberLazyListState(
                        prefetchStrategy = prefetchStrategy,
                    )
                    val imageBOList = presenter.sourceImage.collectAsState(emptyList()).value
                    val listState by presenter.sourceImageSortDate.collectAsState()
                    val tags by presenter.tagsFlow.collectAsState(emptyList())
                    val selectedTags by presenter.selectedTags.collectAsState(emptyList())

                    val appbarTitle by remember(key1 = selectedTags) {
                        derivedStateOf {
                            if (selectedTags.isEmpty()) {
                                getComposeTitle()
                            } else {
                                "\" ${selectedTags.joinToString { it.tagValue }} \""
                            }
                        }
                    }

                    GalleryCompose(
                        screenHeight = screenHeight!!,
                        scrollState = scrollState,
                        appbarTitle = appbarTitle,
                        tags = tags,
                        itemMap = listState,
                        isSelectedTags = selectedTags.isEmpty().not(),
                        onRandomPlay = outer@{
                            if (imageBOList.isEmpty()) {
                                activity?.toast("没有图片")
                                return@outer
                            }
                            presenter.randomImageList(imageBOList).apply {
                                // 图片点击处理
                                ReaderActivity.newIntentToGallery(
                                    activity!!,
                                    galleryManager.putTempGallery(this),
                                    first().id,
                                ).apply {
                                    startActivity(this)
                                }
                            }
                        },
                        onClearAllSelected = {
                            presenter.selectedTags.value = emptyList()
                        },
                        onClickTag = presenter::clickTag,
                    ) {
                        val galleryId = galleryManager.putTempGallery(imageBOList)
                        // 图片点击处理
                        ReaderActivity.newIntentToGallery(activity!!, galleryId, it.id).apply {
                            startActivity(this)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GalleryCompose(
        screenHeight: Int,
        modifier: Modifier = Modifier,
        appbarTitle: String = getComposeTitle(),
        scrollState: LazyListState = rememberLazyListState(),
        itemMap: Map<String, List<ImageBO>> = emptyMap(),
        tags: List<TagVo> = emptyList(),
        isSelectedTags: Boolean = false,
        onRandomPlay: (() -> Unit)? = null,
        onClearAllSelected: (() -> Unit)? = null,
        onClickTag: ((TagVo) -> Unit)? = null,
        onClickImage: ((ImageBO) -> Unit)? = null,
    ) {
        val widthPx = remember { mutableIntStateOf(0) }
        val stableWidth by remember(widthPx.intValue) {
            derivedStateOf {
                val newWidth = widthPx.intValue // 从状态变量中获取最新值
                if (abs(widthPx.intValue - newWidth) > 5) newWidth else widthPx.intValue
            }
        }

        val settings: ScrollbarSettings = ScrollbarSettings.Default.copy(
            scrollbarPadding = 10.dp,
            thumbThickness = 10.dp,
        )

        val controller = rememberLazyListStateController(
            state = scrollState,
            thumbMinLength = settings.thumbMinLength,
            thumbMaxLength = settings.thumbMaxLength,
            alwaysShowScrollBar = settings.alwaysShowScrollbar,
            selectionMode = settings.selectionMode,
        )

        val shouldLoad by remember(controller.isSelected.value) {
            derivedStateOf {
                !controller.isSelected.value
            }
        }
        val items by remember(key1 = stableWidth, key2 = itemMap.size, key3 = tags.hashCode()) {
            derivedStateOf {
                val innerList: MutableList<GalleryItem> = mutableListOf()
                itemMap.forEach { pair ->
                    val (title, images) = pair
                    innerList.add(GalleryItem.Header(title))
                    innerList.addAll(
                        ViewUtil.calculateImageRowV2(
                            minHeight = screenHeight / 7,
                            maxHeight = (screenHeight / 2),
                            screenWidth = stableWidth - 16.dpToPx,
                            spacing = 4.dpToPx,
                            maxSize = 7,
                            images = images,
                            acc = emptyList(),
                        ).map {
                            it.text = title
                            it
                        },
                    )
                }

                appbarTitle.let {
                    innerList.add(0, GalleryItem.AppBar(text = it, tags = tags))
                }
                innerList
            }
        }
        LazyColumnScrollbar(
            settings = settings,
            state = scrollState,
            controller = controller,
            indicatorContent = { index, isThumbSelected ->
                if (isThumbSelected) {
                    val label = items.getOrNull(index)?.text ?: index.toString()
                    Text(
                        text = label,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = Color.Gray,
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                    )
                }
            },
        ) {
            GalleryImageFlow(
                state = scrollState,
                modifier = modifier
                    .padding(horizontal = 16.dp)
                    .onSizeChanged {
                        widthPx.intValue = it.width
                    },
                isSelectedTags = isSelectedTags,
                onClearAllSelected = onClearAllSelected,
                onClickTag = onClickTag,
                onRandomPlay = onRandomPlay,
                items = items,
                shouldLoad = shouldLoad,
            ) {
                onClickImage?.invoke(it)
            }
        }
    }
}

@Composable
fun LazyColumnScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    settings: ScrollbarSettings = ScrollbarSettings.Default,
    indicatorContent: (@Composable (index: Int, isThumbSelected: Boolean) -> Unit)? = null,
    controller: LazyListStateController,
    content: @Composable () -> Unit,
) {
    if (!settings.enabled) {
        content()
    } else {
        Box(modifier) {
            content()
            InternalLazyColumnScrollbar(
                controller = controller,
                state = state,
                settings = settings,
                indicatorContent = indicatorContent,
            )
        }
    }
}
