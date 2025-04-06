package eu.kanade.tachiyomi.ui.gallery.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.getRealDate
import eu.kanade.tachiyomi.ui.gallery.GalleryItem
import eu.kanade.tachiyomi.ui.gallery.ViewUtil.pxToDp

@Composable
fun GalleryImageFlow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    spacing: Dp = 4.dp,
    state: LazyListState = rememberLazyListState(),
    items: List<GalleryItem>,
    shouldLoad: Boolean = true,
    onClickImage: ((ImageBO) -> Unit)? = null,
) {
    LazyColumn(
        state = state,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items(
            count = items.size,
            key = { index ->
                when (val item = items[index]) {
                    is GalleryItem.AppBar -> "app_bar"
                    is GalleryItem.Header -> "header_${item.text.hashCode()}"
                    is GalleryItem.Images -> "row_${item.images.joinToString { it.id.toString() }}"
                }
            },
        ) { index ->
            when (val item = items[index]) {
                is GalleryItem.AppBar -> {
                    val actionBarSize = dimensionResource(R.dimen.mainActionBarSize)

                    Box(
                        modifier = Modifier
                            .padding(top = actionBarSize),
                    ) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.headlineLarge.merge(
                                color =
                                LocalTextStyle.current.color,
                            ),
                            modifier = Modifier.padding(
                                top = (
                                    30 + WindowInsets.statusBars.getTop(
                                        LocalDensity.current,
                                    )
                                    ).dp,
                            ),
                        )
                    }
                }

                is GalleryItem.Header -> {
                    Text(
                        text = item.text.getRealDate(),
                        style = LocalTextStyle.current.merge(
                            MaterialTheme.typography.headlineLarge,
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
                                model = if (shouldLoad) {
                                    ImageRequest.Builder(LocalContext.current)
                                        .allowRgb565(true)
                                        .allowHardware(true)
                                        .size(image.width / 3, image.height / 3)
                                        .precision(Precision.INEXACT)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(true)
                                        .lifecycle(LocalLifecycleOwner.current)
                                        .data(image.url)
                                        .build()
                                } else {
                                    ImageRequest.Builder(LocalContext.current)
                                        .allowRgb565(true)
                                        .allowHardware(true)
                                        .size(image.width / 50, image.height / 50)
                                        .precision(Precision.INEXACT)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(true)
                                        .data(image.url)
                                        .lifecycle(LocalLifecycleOwner.current)
                                        .build()
                                },
                                contentDescription = "",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(
                                        width = (item.height.toDouble() / image.height * image.width)
                                            .toInt()
                                            .pxToDp().dp,
                                    )
                                    .clickable {
                                        onClickImage?.invoke(image)
                                    },
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
