package eu.kanade.tachiyomi.ui.gallery.tags

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage

enum class TagCollection(val ratio: Float) {
    SQUARE(1f), PORTRAIT(2f / 3f);

    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
        tagVo: TagVo,
        shape: Shape = MaterialTheme.shapes.extraSmall,
        isExpended: Boolean = false,
        onClick: (() -> Unit)? = null,
        onClickPlay: (() -> Unit)? = null,
    ) {
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpended) 90f else 0f,
            animationSpec = tween(durationMillis = 400),
        )
        Surface(
            modifier = modifier
                .aspectRatio(ratio)
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (tagVo.cover != null) {
                    AsyncImage(
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        model = tagVo.cover?.url,
                        filterQuality = FilterQuality.Low,
                    )
                }
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(
                                alpha = 0.6f,
                            ),
                        ),
                ) {
                    val (tagRef, countMsgRef, playRef) = createRefs()
                    Text(
                        modifier = Modifier.constrainAs(tagRef) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        },
                        text = tagVo.name,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (tagVo.images.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier
                                .rotate(rotationAngle)
                                .clickable {
                                    onClickPlay?.invoke()
                                }
                                .constrainAs(playRef) {
                                    bottom.linkTo(parent.bottom)
                                    end.linkTo(parent.end)
                                }
                                .padding(4.dp),
                        )
                        Text(
                            modifier = Modifier
                                .constrainAs(countMsgRef) {
                                    start.linkTo(parent.start)
                                    bottom.linkTo(parent.bottom)
                                }
                                .padding(start = 4.dp, bottom = 4.dp),
                            text = tagVo.images.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
