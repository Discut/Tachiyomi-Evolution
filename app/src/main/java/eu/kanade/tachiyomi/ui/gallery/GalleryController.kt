package eu.kanade.tachiyomi.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.databinding.GalleryControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.system.toInt
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.fullAppBarHeight
import eu.kanade.tachiyomi.util.view.setAppBarBG
import uy.kohesive.injekt.injectLazy

class GalleryController(bundle: Bundle? = null) :
    BaseCoroutineController<GalleryControllerBinding, GalleryPresenter>(bundle),
    RootSearchInterface {

    private val galleryManager: GalleryManager by injectLazy()

    override val presenter = GalleryPresenter()

    override fun createBinding(inflater: LayoutInflater): GalleryControllerBinding {
        return GalleryControllerBinding.inflate(inflater)
    }

    override fun getTitle(): String? =
        view?.context?.getString(R.string.gallery)

    override fun getSearchTitle(): String {
        return "search"
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        val bigToolbarHeight = fullAppBarHeight ?: 0
        val insets = activity?.window?.decorView?.rootWindowInsetsCompat
        /*        view.rootView.updatePadding(
                    top = bigToolbarHeight + (insets?.getInsets(systemBars())?.top ?: 0),
                    bottom = insets?.getInsets(systemBars())?.bottom ?: 0,
                )*/
        view.findViewById<ComposeView>(R.id.gallery_container)?.setContent {
            val isDarkTheme = when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> isSystemInDarkTheme() // You can define this function to check system theme preference if needed
            }
            MaterialTheme {
                ProvideTextStyle(
                    value = LocalTextStyle.current.copy(
                        color = if (isDarkTheme) Color.White else Color.Black,
                    ),
                ) {
                    BoxWithConstraints {
                        GalleryCompose(
                            bigToolbarHeight + (insets?.getInsets(systemBars())?.top ?: 0),
                            insets?.getInsets(systemBars())?.bottom ?: 0,
                            maxWidth.value.dpToPx.toInt(),
                            maxHeight.value.dpToPx.toInt(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun GalleryCompose(
        topPadding: Int = 0,
        bottomPadding: Int = 0,
        maxWith: Int,
        screenHeight: Int,
        modifier: Modifier = Modifier,
    ) {
        val scrollState = rememberLazyListState()
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // 状态管理
        var appBarHeight by remember { mutableStateOf(0) }
        var isToolbarColored by remember { mutableStateOf(false) }
        var bottomNavOffset by remember { mutableStateOf(0f) }

        // 计算滚动位置
        val isAtTop by remember {
            derivedStateOf { scrollState.firstVisibleItemIndex == 0 }
        }

        // 计算总滚动距离（派生状态优化性能）
        val scrollOffset by remember {
            derivedStateOf {
                calculateScrollOffset(scrollState)
            }
        }

        // 动态边距计算
        val contentPadding = remember(topPadding, bottomPadding) {
            PaddingValues(
                top = with(density) { (topPadding + appBarHeight).toDp() },
                bottom = with(density) { (bottomPadding + 100).toDp() },
            )
        }

        // 滚动联动处理
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // 应用栏高度调整逻辑
                    val delta = available.y
                    appBarHeight =
                        (appBarHeight - delta).coerceIn(0.0F, 128.dpToPx.toFloat()).toInt()

                    // 底部导航栏隐藏逻辑
                    if (delta > 0) {
                        bottomNavOffset =
                            (bottomNavOffset + delta).coerceAtMost(64.dpToPx.toFloat())
                    } else {
                        bottomNavOffset = (bottomNavOffset + delta).coerceAtLeast(0f)
                    }

                    // 颜色渐变逻辑
                    isToolbarColored =
                        !isAtTop && -scrollOffset > 48.dpToPx.toFloat()
                    return Offset.Zero
                }
            }
        }

        var currentPos by rememberSaveable { mutableIntStateOf(scrollState.firstVisibleItemIndex) }

        // 生命周期处理
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        // 保存滚动状态
                        currentPos = scrollState.firstVisibleItemIndex
                    }

                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(key1 = scrollOffset) {
            if (scrollOffset < 48.dpToPx.toFloat()) {
                activityBinding?.appBar?.updateAppBarAfterY(-scrollOffset)
            }
        }
        LaunchedEffect(key1 = isToolbarColored) {
            setAppBarBG(isToolbarColored.toInt().toFloat())
        }
        val listState by presenter.sourceImageSortDate.collectAsState()
        // 内容布局
        GalleryImageFlow(
            state = scrollState,
            modifier = modifier
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 16.dp),
            contentPadding = contentPadding,
            maxWith = maxWith - 16.dpToPx,
            screenHeight = screenHeight,
            data = listState,
        ) {
            val galleryId = galleryManager.putTempGallery(presenter.sourceImage.value)
            // 图片点击处理
            ReaderActivity.newIntentToGallery(activity!!, galleryId, 1).apply {
                startActivity(this)
            }
        }

        /*// 应用栏颜色动画
        AnimatedContent(targetState = isToolbarColored) { colored ->
            Surface(
                color = animateColorAsState(
                    if (colored) MaterialTheme.colors.primary
                    else MaterialTheme.colors.surface
                ).value,
                elevation = animateDpAsState(if (colored) 8.dp else 0.dp).value
            ) {
                // 应用栏内容实现
            }
        }

        // 底部导航栏动画
        AnimatedVisibility(
            visible = bottomNavOffset < 48.dp.toPx(),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            BottomNavigation(
                modifier = Modifier.offset(y = with(density) { bottomNavOffset.toDp() })
            ) {
                // 底部导航项实现
            }
        }*/
    }

    // 计算总滚动距离的方法
    private fun calculateScrollOffset(state: LazyListState): Int {
        return state
            .layoutInfo
            .visibleItemsInfo
            .firstOrNull()
            ?.let { firstVisibleItem ->
                // 计算所有已完全滚动过去项的总高度
                val pastItemsHeight = state.layoutInfo.visibleItemsInfo
                    .takeWhile { it.index < firstVisibleItem.index }
                    .sumOf { it.size }

                // 当前可见项的滚动偏移量
                val currentItemOffset = firstVisibleItem.offset

                // 总滚动距离 = 已过去项总高度 + 当前项偏移量
                pastItemsHeight + currentItemOffset
            } ?: 0
    }
}
