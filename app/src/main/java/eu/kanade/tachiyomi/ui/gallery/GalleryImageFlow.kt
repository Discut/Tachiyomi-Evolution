package eu.kanade.tachiyomi.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.ui.gallery.ViewUtil.pxToDp
import eu.kanade.tachiyomi.util.system.dpToPx

@Composable
fun GalleryImageFlow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    spacing: Dp = 4.dp,
    state: LazyListState = rememberLazyListState(),
    maxWith: Int,
    screenHeight: Int,
    data: Map<String, List<ImageBO>>,
) {
    val items by remember(key1 = maxWith, key2 = data) {
        derivedStateOf {
            val innerList: MutableList<GalleryItem> = mutableListOf()
            data.forEach {
                innerList.add(GalleryItem.Header(it.key))
                innerList.addAll(
                    ViewUtil.calculateImageRow(
                        minHeight = screenHeight / 5,
                        maxHeight = screenHeight / 3,
                        screenWidth = maxWith,
                        spacing = spacing.value.dpToPx.toInt(),
                        maxSize = 8,
                        images = it.value,
                        acc = emptyList(),
                    ),
                )
            }
            innerList
        }
    }
    LazyColumn(
        state = state,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items(count = items.size, key = { it.hashCode() }) { index ->
            when (val item = items[index]) {
                is GalleryItem.Header -> {
                    Text(
                        text = item.text,
                        style = LocalTextStyle.current.merge(
                            MaterialTheme.typography.titleLarge,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 72.dp, bottom = 16.dp),
                    )
                }

                is GalleryItem.Images -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(item.height.pxToDp().dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        item.images.forEach { image ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(true)
                                    .data(image.url)
                                    .build(),
                                contentDescription = "",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(
                                        width = (item.height.toDouble() / image.height * image.width)
                                            .toInt()
                                            .pxToDp().dp,
                                    ),
                                // 灰色占位符
                                placeholder = ColorPainter(Color.DarkGray),
                                filterQuality = FilterQuality.Low,
                                contentScale = ContentScale.FillHeight,
                            )
                        }
                    }
                }
            }
        }
    }
}
