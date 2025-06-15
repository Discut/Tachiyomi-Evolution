package eu.kanade.tachiyomi.ui.gallery.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.databinding.GalleryControllerBinding
import eu.kanade.tachiyomi.theme.GalleryTheme
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.generateTimestampBasedID
import eu.kanade.tachiyomi.util.system.pxToDp
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.ceil

class TagsController(
    bundle: Bundle? = null,
) : BaseCoroutineController<GalleryControllerBinding, TagsPresenter>(bundle),
    RootSearchInterface {

    private val galleryManager = Injekt.get<GalleryManager>()

    companion object {
        const val ID = "TagsController"
    }

    override val presenter by lazy {
        TagsPresenter()
    }

    override fun createBinding(inflater: LayoutInflater): GalleryControllerBinding =
        GalleryControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        activity?.findViewById<View>(R.id.app_bar)?.visibility = View.GONE
        view.findViewById<ComposeView>(R.id.gallery_container)?.setContent {
            GalleryTheme {
                val tags by presenter.tagsVoFlow.collectAsState(emptyList())

                val scrollBehavior =
                    TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
                val isCollapsed by remember {
                    derivedStateOf { scrollBehavior.state.collapsedFraction > 0.5f }
                }

                BackHandler {
                    router.popCurrentController()
                }
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
                                    context.getString(R.string.gallery_tag),
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
                                        // router.setRoot((lastController ?: GalleryController()).withFadeInTransaction())
                                        router.popCurrentController()
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
                    var selected by remember {
                        mutableStateOf<TagVo?>(null)
                    }
                    Content(
                        modifier = Modifier.fillMaxSize(),
                        contentPaddingValues = innerPadding,
                        needExpandTag = selected,
                        tags = tags,
                        onClickPlay = { tagVo, index ->
                            val id = generateTimestampBasedID()
                            // 图片点击处理
                            ReaderActivity.newIntentToGallery(
                                activity!!,
                                galleryManager.putTempGallery(tagVo.images, id),
                                tagVo.images[index].id,
                            ).apply {
                                startActivity(this)
                            }
                        },

                        onClickTag = { tagVo ->
                            selected = if (selected == tagVo) {
                                null
                            } else {
                                tagVo
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun Content(
    modifier: Modifier,
    contentPaddingValues: PaddingValues = PaddingValues(),
    columns: Int = 5,
    tags: List<TagVo> = emptyList(),
    needExpandTag: TagVo? = null,
    onClickPlay: ((TagVo, Int) -> Unit),
    onClickTag: ((TagVo) -> Unit),
) {
    val tagsPair by remember(tags, needExpandTag, columns) {
        derivedStateOf {
            if (needExpandTag == null) {
                return@derivedStateOf tags to emptyList()
            }
            val needExpandTagIndex = tags.indexOf(needExpandTag).takeIf { it >= 0 }
                ?: return@derivedStateOf tags to emptyList()

            val position = ceil((needExpandTagIndex + 1f) / columns).toInt() * columns

            tags.subList(0, position.coerceAtMost(tags.size)) to
                tags.subList(position.coerceAtMost(tags.size), tags.size)
        }
    }

    ProvideTextStyle(
        value = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        val heights = remember { mutableStateListOf<Float>() }
        val max = 60
        fun addHeight(height: Float) {
            if (heights.isNotEmpty() && heights.first() == height) {
                return
            }
            if (heights.size >= max) {
                heights.removeAt(0)
            }
            heights.add(height)
        }

        val realHeight by remember(heights) {
            derivedStateOf {
                heights.max()
            }
        }

        LazyVerticalGrid(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                ),
            contentPadding = PaddingValues(
                top = contentPaddingValues.calculateTopPadding(),
                bottom = contentPaddingValues.calculateBottomPadding() + 16.dp,
            ),
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Items
            val (top, bottom) = tagsPair
            items(count = top.size, key = { index -> top[index].tagId }) { index ->
                val tag = top[index]
                TagCollection.SQUARE(
                    modifier = Modifier
                        .animateItem()
                        .zIndex(2f)
                        .onGloballyPositioned {
                            addHeight(it.size.height.toFloat())
                        },
                    tagVo = tag,
                    isExpended = tag == needExpandTag,
                    onClickPlay = {
                        onClickPlay.invoke(tag, 0)
                    },
                    onClick = {
                        onClickTag.invoke(tag)
                    },
                )
            }

            if (needExpandTag == null) {
                return@LazyVerticalGrid
            }

            item(span = { GridItemSpan(columns) }) {
                ImageView(
                    realHeight = realHeight,
                    needExpandTag = needExpandTag,
                    onClickPlay = onClickPlay,
                )
            }
            items(count = bottom.size, key = { index -> bottom[index].tagId }) { index ->
                val tag = bottom[index]
                TagCollection.SQUARE(
                    modifier = Modifier
                        .animateItem()
                        .zIndex(2f),
                    tagVo = tag,
                    onClickPlay = {
                        onClickPlay.invoke(tag, 0)
                    },
                    onClick = {
                        onClickTag.invoke(tag)
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    Content(
        modifier = Modifier,
        onClickPlay = { l, r -> },
        onClickTag = {
        },
    )
}

@Composable
fun ImageView(
    modifier: Modifier = Modifier,
    realHeight: Float,
    needExpandTag: TagVo,
    onClickPlay: ((TagVo, Int) -> Unit),
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(realHeight.pxToDp.dp)
            .zIndex(1f),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            count = needExpandTag.images.size,
            key = { index -> needExpandTag.images[index].id + needExpandTag.tagId },
        ) { index ->
            val image = needExpandTag.images[index]

            Surface(
                modifier = Modifier
                    .animateItem()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable {
                        onClickPlay.invoke(needExpandTag, index)
                    },
            ) {
                AsyncImage(
                    modifier = Modifier
                        .animateItem(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    model = image.url,
                    filterQuality = FilterQuality.Low,
                )
            }
        }
    }
}
