package eu.kanade.tachiyomi.ui.gallery.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.UnionImageBO
import eu.kanade.tachiyomi.model.getRealDate
import eu.kanade.tachiyomi.ui.gallery.ViewUtil
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryItem
import eu.kanade.tachiyomi.ui.gallery.plus
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.pxToDp
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import my.nanihadesuka.compose.controller.LazyListStateController
import my.nanihadesuka.compose.controller.rememberLazyListStateController
import uy.kohesive.injekt.injectLazy
import kotlin.math.abs

@Composable
fun GalleryCompose(
    screenHeight: Int,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
    appbarTitle: String,
    scrollState: LazyListState = rememberLazyListState(),
    itemMap: Map<String, List<IImageBo>> = emptyMap(),
    tags: List<TagVo> = emptyList(),
    isSelectedTags: Boolean = false,
    onRandomPlay: (() -> Unit)? = null,
    onClearAllSelected: (() -> Unit)? = null,
    onClickJumpToTags: (() -> Unit)? = null,
    onClickTag: ((TagVo) -> Unit)? = null,
    onPlayGallery: ((IImageBo, source: List<IImageBo>) -> Unit)? = null,
    onDeleteImages: ((List<IImageBo>) -> Unit)? = null,
    onChangeImagesVisible: ((List<IImageBo>, Boolean) -> Unit)? = null,
    onSplitImages: ((List<UnionImageBO>) -> Unit)? = null,
    onMergeImages: ((List<IImageBo>) -> Unit)? = null,
    onEditUnionImage: (UnionImageBO) -> Unit,
) {
    val preference: PreferencesHelper by remember {
        injectLazy()
    }
    val widthPx = remember { mutableIntStateOf(0) }
    val stableWidth by remember(widthPx.intValue) {
        derivedStateOf {
            val newWidth = widthPx.intValue // 从状态变量中获取最新值
            if (abs(widthPx.intValue - newWidth) > 5) newWidth else widthPx.intValue
        }
    }

    val selectedImages = remember { mutableStateListOf<IImageBo>() }

    val isSelectMode by remember(selectedImages) {
        derivedStateOf {
            selectedImages.isNotEmpty()
        }
    }

    BackHandler(enabled = isSelectMode) {
        selectedImages.clear()
    }

    val isShowScrollToTop by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 3
        }
    }

    var isShowMoreEditSheet by remember { mutableStateOf(false) }
    var isShowSettings by remember { mutableStateOf(true) }
    var isShowSettingsMore by remember { mutableStateOf(false) }
    var isScoringUp = scrollState.isScrollingUp()
    val isShowHideImage by preference.isShowHideImages().asFlow().collectAsState(true)
    val isShowDiffGroup by preference.isMergeDiffImage().asFlow().collectAsState(true)
    val rowImageMaxSize by preference.getRowImageMaxSize().asFlow().collectAsState(7)

    val settings: ScrollbarSettings = ScrollbarSettings.Default.copy(
        scrollbarPadding = 10.dp,
        thumbThickness = 10.dp,
        thumbMaxLength = 0.3f,
        thumbUnselectedColor = MaterialTheme.colorScheme.primaryContainer,
        thumbSelectedColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    val controller = rememberLazyListStateController(
        state = scrollState,
        thumbMinLength = settings.thumbMinLength,
        thumbMaxLength = settings.thumbMaxLength,
        alwaysShowScrollBar = settings.alwaysShowScrollbar,
        selectionMode = settings.selectionMode,
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    var isShowSettingSheet by remember { mutableStateOf(false) }
    val settingSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    val scope = rememberCoroutineScope()

    val compositeKey = remember(itemMap, rowImageMaxSize) {
        itemMap.hashCode() xor rowImageMaxSize.hashCode()
    }
    val items by remember(key1 = stableWidth, key2 = compositeKey, key3 = tags.hashCode()) {
        derivedStateOf {
            val innerList: MutableList<GalleryItem> = mutableListOf()
            itemMap.forEach { pair ->
                val (title, images) = pair
                innerList.add(GalleryItem.Header(title, images))
                innerList.addAll(
                    ViewUtil.calculateImageRowV2(
                        minHeight = screenHeight / 7,
                        maxHeight = (screenHeight / 2),
                        screenWidth = stableWidth - 16.dpToPx,
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

            appbarTitle.let {
                innerList.add(0, GalleryItem.AppBar(text = it, tags = tags))
            }
            innerList
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumnScrollbar(
            settings = settings,
            state = scrollState,
            controller = controller,
            scrollBarPaddingValues = PaddingValues(
                top = (screenHeight / 5).pxToDp.dp,
                bottom = paddingValues.calculateBottomPadding(),
            ),
            indicatorContent = { index, isThumbSelected ->
                if (isThumbSelected) {
                    val label = items.getOrNull(index)?.text ?: index.toString()
                    Text(
                        text = label.getRealDate(),
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
                contentPadding = paddingValues,
                isSelectedTags = isSelectedTags,
                selectedImages = selectedImages.toSet(),
                isSelectMode = isSelectMode,
                onClickJumpToTags = onClickJumpToTags,
                onClearAllSelected = onClearAllSelected,
                onClickTag = onClickTag,
                onRandomPlay = onRandomPlay,
                items = items,
                onClickImage = {
                    if (it.isEmpty()) {
                        return@GalleryImageFlow
                    }
                    if (!isSelectMode) {
                        onPlayGallery?.invoke(it[0], emptyList())
                    } else {
                        selectedImages.progress(it)
                    }
                },
                onLongClickImage = {
                    selectedImages.progress(it)
                },
            )
        }

        AnimatedVisibility(
            visible = isSelectMode,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = paddingValues.calculateTopPadding()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            selectedImages.clear()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = LocalTextStyle.current.color,
                        )
                    }

                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = selectedImages.size.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = LocalTextStyle.current.color,
                        ),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (selectedImages.isEmpty()) {
                                return@IconButton
                            }
                            onPlayGallery?.invoke(selectedImages[0], selectedImages)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "play",
                            tint = LocalTextStyle.current.color,
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedImages.size == 1 && selectedImages.first() is UnionImageBO,
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedImages.isEmpty() && selectedImages.first() !is UnionImageBO) {
                                    return@IconButton
                                }
                                val bo = selectedImages.first()
                                if (bo is UnionImageBO) {
                                    onEditUnionImage.invoke(bo)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "edit",
                                tint = LocalTextStyle.current.color,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isShowMoreEditSheet = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "more",
                            tint = LocalTextStyle.current.color,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(PaddingValues(32.dp) + paddingValues),
            visible = isShowScrollToTop,
            enter = fadeIn() + slideInVertically {
                it
            },
            exit = fadeOut() + slideOutVertically {
                it
            },
        ) {
            FloatingActionButton(
                backgroundColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = FloatingActionButtonDefaults.extendedFabShape,
                onClick = {
                    scope.launchUI {
                        scrollState.animateScrollToItem(0)
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerticalAlignTop,
                    contentDescription = "Scroll to top",
                    tint = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 32.dp),
            visible = isShowSettings && !isSelectMode && isScoringUp,
            enter = fadeIn() + slideInVertically {
                -it
            },
            exit = fadeOut() + slideOutVertically {
                -it
            },
        ) {
            FloatingSettings(
                isShowMore = isShowSettingsMore,
                alwaysShowCompose = {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        onClick = {
                            isShowSettingSheet = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = LocalTextStyle.current.color,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }

        if (isShowMoreEditSheet) {
            GalleryEditBottomSheet(
                sheetState = sheetState,
                selectedImages = selectedImages,
                tags = tags,
                onDeleteImages = {
                    isShowMoreEditSheet = false
                    onDeleteImages?.invoke(it.toList())
                    selectedImages.clear()
                },
                onDismissRequest = {
                    isShowMoreEditSheet = false
                },
                onHideImages = {
                    isShowMoreEditSheet = false
                    onChangeImagesVisible?.invoke(it.toList(), false)
                    selectedImages.clear()
                },
                onShowImages = {
                    isShowMoreEditSheet = false
                    onChangeImagesVisible?.invoke(it.toList(), true)
                    selectedImages.clear()
                },
                onSplitImages = {
                    isShowMoreEditSheet = false
                    onSplitImages?.invoke(it.toList())
                    selectedImages.clear()
                },
                onMergeImages = {
                    isShowMoreEditSheet = false
                    onMergeImages?.invoke(it.toList())
                    selectedImages.clear()
                },
            )
        }

        if (isShowSettingSheet) {
            SettingsBottomSheet(
                sheetState = settingSheetState,
                isShowHideImage = isShowHideImage,
                isShowDiffGroup = isShowDiffGroup,
                imageRowSize = rowImageMaxSize,
                onClickHideImage = {
                    preference.isShowHideImages().set(it)
                },
                onClickShowDiffGroup = {
                    preference.isMergeDiffImage().set(it)
                },
                onDismissRequest = {
                    isShowSettingSheet = false
                },
                onImageRowSizeChange = {
                    preference.getRowImageMaxSize().set(it)
                },
            )
        }
    }
}

@Composable
fun LazyColumnScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    scrollBarPaddingValues: PaddingValues = PaddingValues(),
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
                modifier = Modifier.padding(scrollBarPaddingValues),
                controller = controller,
                state = state,
                settings = settings,
                indicatorContent = indicatorContent,
            )
        }
    }
}

private fun SnapshotStateList<IImageBo>.progress(images: List<IImageBo>) {
    if (images.size == 1) {
        if (this.any { it.id == images[0].id }) {
            removeIf { it.id == images[0].id }
        } else {
            add(images[0])
        }
        return
    }
    if (images.any { out -> !this.any { it.id == out.id } }) {
        images.forEach { out ->
            if (!this.any { it.id == out.id }) {
                add(out)
            }
        }
    } else {
        removeAll { out ->
            images.any { it.id == out.id }
        }
    }
}

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    // 记录上一次的索引和偏移量
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }

    return remember(this) {
        derivedStateOf {
            val direction = if (previousIndex != firstVisibleItemIndex) {
                // 索引变化时判断方向
                previousIndex > firstVisibleItemIndex
            } else {
                // 索引不变时通过偏移量判断
                previousOffset >= firstVisibleItemScrollOffset
            }
            // 更新记录值
            previousIndex = firstVisibleItemIndex
            previousOffset = firstVisibleItemScrollOffset
            direction
        }
    }.value
}
