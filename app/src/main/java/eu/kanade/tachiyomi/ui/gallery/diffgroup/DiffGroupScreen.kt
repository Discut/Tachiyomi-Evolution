package eu.kanade.tachiyomi.ui.gallery.diffgroup

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import coil.compose.AsyncImage
import eu.kanade.tachiyomi.ui.gallery.diffgroup.domain.DiffGroupState
import eu.kanade.tachiyomi.ui.gallery.diffgroup.domain.DiffImageVo
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
internal fun DiffGroupScreen(
    state: DiffGroupState,
    onChangedImageOrder: (List<DiffImageVo>) -> Unit,
    onBack: () -> Unit,
) {
    when (state) {
        is DiffGroupState.Content -> {
            DiffGroupContent(state, onBack = onBack, onChangedImageOrder = onChangedImageOrder)
        }

        is DiffGroupState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        DiffGroupState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        else -> {}
    }
}

@Composable
internal fun DiffGroupContent(
    state: DiffGroupState.Content,
    onChangedImageOrder: (List<DiffImageVo>) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            val context = LocalContext.current
            // 根据滚动比例动态缩放标题
            val scale = 1f - (scrollBehavior.state.collapsedFraction * 0.5f)
            val animatedScale by animateFloatAsState(
                targetValue = scale.coerceIn(0.7f, 1f),
                animationSpec = snap(),
            )
            LargeTopAppBar(
                expandedHeight = 160.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Text(
                        text = state.diffGroup.groupName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineLarge.merge(
                            TextStyle(
                                fontFamily = FontFamily.Default,
                                color = LocalTextStyle.current.color,
                                lineHeight = 32.sp, // 根据设计系统调整
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                        ),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBack()
                            // router.setRoot((lastController ?: GalleryController()).withFadeInTransaction())
                            // router.popCurrentController()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // do something
                    },) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Localized description",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        ProvideTextStyle(
            value = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
        ) {
            var list by remember { mutableStateOf(state.images) }
            LaunchedEffect(key1 = state) {
                list = state.images
            }
            val lazyGridState = rememberLazyGridState()
            val view = LocalView.current
            val reorderableLazyGridState =
                rememberReorderableLazyGridState(lazyGridState) { from, to ->
                    if (from.index == to.index) return@rememberReorderableLazyGridState // 跳过无效移动
                    list = list.toMutableList().apply {
                        val positionOrder = get(to.index).order

                        val otherOrder = if (from.index > to.index) {
                            if (to.index - 1 < 0) {
                                BigDecimal(1000)
                            } else {
                                get(to.index - 1).order
                            }
                        } else {
                            if (to.index + 1 >= size) {
                                last().order + BigDecimal(1000)
                            } else {
                                get(to.index + 1).order
                            }
                        }
                        add(
                            to.index,
                            removeAt(from.index).copy(
                                order = (positionOrder + otherOrder).divide(
                                    BigDecimal(2),
                                    RoundingMode.HALF_UP,
                                ),
                            ),
                        )
                    }

                    ViewCompat.performHapticFeedback(
                        view,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
                        } else {
                            HapticFeedbackConstantsCompat.CLOCK_TICK
                        },
                    )
                }
            LazyVerticalGrid(
                state = lazyGridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                    ),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(count = list.size, key = { index -> list[index].image.id }) { index ->
                    ReorderableItem(
                        reorderableLazyGridState,
                        key = list[index].image.id,
                    ) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                        val image = list[index]
                        Surface(shadowElevation = elevation) {
                            AsyncImage(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .animateItem()
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                            ViewCompat.performHapticFeedback(
                                                view,
                                                HapticFeedbackConstantsCompat.GESTURE_START,
                                            )
                                        },
                                        onDragStopped = {
                                            ViewCompat.performHapticFeedback(
                                                view,
                                                HapticFeedbackConstantsCompat.GESTURE_END,
                                            )
                                            onChangedImageOrder(list)
                                        },
                                    ),
                                contentDescription = "",
                                contentScale = ContentScale.Crop,
                                model = image.image.url,
                                filterQuality = FilterQuality.Low,
                            )
                        }
                    }
                }
            }
        }
    }
}
