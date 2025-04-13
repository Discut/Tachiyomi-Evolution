package eu.kanade.tachiyomi.ui.gallery.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.getRealDate
import eu.kanade.tachiyomi.ui.gallery.ViewUtil.pxToDp
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryItem
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo

@Composable
fun GalleryImageFlow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    spacing: Dp = 4.dp,
    state: LazyListState = rememberLazyListState(),
    items: List<GalleryItem>,
    isSelectedTags: Boolean = false,
    onClickTag: ((TagVo) -> Unit)? = null,
    onClearAllSelected: (() -> Unit)? = null,
    onClickJumpToTags: (() -> Unit)? = null,
    onRandomPlay: (() -> Unit)? = null,
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
                    TitleText(
                        title = item.text,
                    ) {
                        if (item.tags.isEmpty()) {
                            return@TitleText
                        }

                        TagVerticalSelector(
                            modifier = Modifier
                                .fillMaxWidth(),
                            actions = {
                                IconButton(
                                    onClick = {
                                        onRandomPlay?.invoke()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SmartDisplay,
                                        contentDescription = null,
                                        tint = LocalTextStyle.current.color,
                                    )
                                }

                                AnimatedVisibility(visible = isSelectedTags) {
                                    IconButton(
                                        onClick = {
                                            onClearAllSelected?.invoke()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteForever,
                                            contentDescription = null,
                                            tint = LocalTextStyle.current.color,
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        onClickJumpToTags?.invoke()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        tint = LocalTextStyle.current.color,
                                    )
                                }
                            },
                        ) {
                            items(
                                key = { it.hashCode() },
                                count = item.tags.size,
                            ) { index ->
                                val tag = item.tags[index]
                                FilterChip(
                                    onClick = {
                                        onClickTag?.invoke(tag)
                                    },
                                    selected = tag.isSelected,
                                    selectedIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Done,
                                            contentDescription = "Done icon",
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    },
                                ) {
                                    Text(
                                        text = tag.tagValue,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
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
                            /*val source: Any =
                                if (image.hasThumbnail && imageCache.isExist(image.id.toString())) {
                                    image
                                } else {
                                    image.url
                                }*/
                            val context = LocalContext.current
                            val lifecycleOwner = LocalLifecycleOwner.current

                            val requestBuilder by remember {
                                mutableStateOf(
                                    ImageRequest.Builder(context)
                                        .allowRgb565(true)
                                        .allowHardware(true)
                                        .precision(Precision.INEXACT)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(true)
                                        .lifecycle(lifecycleOwner),
                                )
                            }

                            /*val request by remember(key1 = shouldLoad, key2 = image) {
                                derivedStateOf {
                                    when {
                                        image.hasThumbnail -> {
                                            requestBuilder.data(source).build()
                                        }

                                        else -> {
                                            requestBuilder.size(image.width / 3, image.height / 3)
                                                .data(source).build()
                                        }
                                    }
                                }
                            }*/

                            AsyncImage(
                                model = requestBuilder.size(image.width / 3, image.height / 3)
                                    .data(image.url).build(),
                                contentDescription = "",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(
                                        width = (item.height.toDouble() * image.aspectRatio)
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
