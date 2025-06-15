package eu.kanade.tachiyomi.ui.gallery.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.first
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Random

@Composable
fun TagListCompose(
    modifier: Modifier = Modifier,
    tags: List<TagVo>,
) {
    val tagMap = remember {
        mutableStateMapOf<Long, String>()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(tags) {
        launchIO {
            val galleryManager: GalleryManager = Injekt.get()
            galleryManager.getAllTagsVoAsFlow().first().forEach {
                it.cover = if (it.images.isNotEmpty()) {
                    it.images.shuffled(Random(System.currentTimeMillis())).first()
                } else {
                    null
                }
                tagMap[it.tagId] = it.cover?.url ?: ""
            }
        }
    }

    val requestBuilder by remember {
        mutableStateOf(
            ImageRequest.Builder(context)
                .allowRgb565(false)
                .allowHardware(true)
                .precision(Precision.INEXACT)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .lifecycle(lifecycleOwner),
        )
    }

    // 新增滚动状态
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        items(
            count = tags.size,
            key = { index -> tags[index].tagId },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 1.dp),
            ) {
                val firstItemTranslationY by remember(listState) {
                    derivedStateOf {
                        getParallaxOffset(listState, it)
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize(),
                    /*.graphicsLayer {
                        translationY = firstItemTranslationY * 10
                    }
                    .graphicsLayer { alpha = 0.9f }*/
                ) {
                    AsyncImage(
                        model = requestBuilder.data(tagMap[tags[it].tagId]).build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(),
                        filterQuality = FilterQuality.Low,
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    text = tags[it].tagValue,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterStart),
                )
            }
        }
    }
}

private fun getParallaxOffset(
    listState: LazyListState,
    itemIndex: Int,
): Float {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    return visibleItems
        .firstOrNull { it.index == itemIndex }
        ?.let { itemInfo ->
            // 计算项在屏幕中的位置比例
            val itemOffset = itemInfo.offset
            val viewportHeight = layoutInfo.viewportEndOffset
            val scrollProgress = (itemOffset / viewportHeight.toFloat()) * 2
            scrollProgress.coerceIn(-1f, 1f) * 30f // 视差强度系数
        } ?: 0f
}
