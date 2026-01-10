package eu.kanade.tachiyomi.ui.gallery.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WebStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.UnionImageBO
import eu.kanade.tachiyomi.ui.gallery.ViewUtil.pxToDp

@Composable
fun ImageCompose(
    modifier: Modifier = Modifier,
    image: IImageBo,
    height: Int,
    isSelected: Boolean,
    onClick: ((IImageBo) -> Unit)? = null,
    onLongClick: ((IImageBo) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(
                width = (height.toDouble() * image.aspectRatio)
                    .toInt()
                    .pxToDp().dp,
            )
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            )
            .combinedClickable(
                onClick = {
                    onClick?.invoke(image)
                },
                onLongClick = {
                    onLongClick?.invoke(image)
                },
            ),
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val requestBuilder by remember {
            mutableStateOf(
                ImageRequest.Builder(context)
                    .allowRgb565(true)
                    .allowHardware(true)
                    .precision(Precision.INEXACT)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .lifecycle(lifecycleOwner),
            )
        }

        if (image is UnionImageBO) {
            HorizontalUncontainedCarousel(
                state = rememberCarouselState { image.unions.count() },
                modifier = Modifier.fillMaxSize(),
                itemWidth = image.unions.first().width.pxToDp().dp,
                itemSpacing = 2.dp,
            ) { index ->
                val union = image.unions[index]
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = requestBuilder.data(union).build(),
                    contentDescription = "",
                    // 灰色占位符
                    placeholder = ColorPainter(Color.DarkGray),
                    filterQuality = FilterQuality.Low,
                    contentScale = ContentScale.FillHeight,
                )
            }
        } else {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = requestBuilder.data(image).build(),
                contentDescription = "",
                // 灰色占位符
                placeholder = ColorPainter(Color.DarkGray),
                filterQuality = FilterQuality.Low,
                contentScale = ContentScale.FillHeight,
            )
        }

        if (image.isHide) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.padding(2.dp),
                    imageVector = Icons.Default.VisibilityOff,
                    tint = Color.White,
                    contentDescription = "",
                )
            }
        }

        if (image is UnionImageBO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp, end = 4.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.padding(2.dp),
                    imageVector = Icons.Default.WebStories,
                    tint = Color.White,
                    contentDescription = "diff group",
                )
            }
        }
    }
}
