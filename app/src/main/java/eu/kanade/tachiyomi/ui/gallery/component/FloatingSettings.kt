package eu.kanade.tachiyomi.ui.gallery.component

//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FloatingSettings(
    modifier: Modifier = Modifier,
    isShowMore: Boolean,
    alwaysShowCompose: @Composable RowScope.() -> Unit,
    moreCompose: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        if (moreCompose != null) {
            AnimatedVisibility(
                visible = isShowMore,
                enter = (
                    fadeIn(
                        animationSpec = tween(durationMillis = 200),
                    ) + slideInHorizontally(
                        animationSpec = tween(durationMillis = 200),
                        initialOffsetX = {
                            500
                        },
                    )
                    ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = 200),
                ) + slideOutHorizontally(
                    animationSpec = tween(durationMillis = 200),
                    targetOffsetX = {
                        500
                    },
                ),
            ) {
                moreCompose()
            }
        }
        alwaysShowCompose()
    }
}
