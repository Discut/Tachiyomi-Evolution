package eu.kanade.tachiyomi.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Chip
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.model.TagBo

@Composable
fun TagsAndEdit(
    modifier: Modifier = Modifier,
    tags: List<TagBo>,
    isLoaded: Boolean,
    onClickEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .padding(end = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        tags.reversed().forEachIndexed { index, tag ->
            AnimatedVisibility(
                visible = isLoaded,
                enter = (
                    fadeIn(
                        animationSpec = tween(durationMillis = 200 + (tags.size - index) * 50),
                    ) + slideInHorizontally(
                        animationSpec = tween(durationMillis = 200 + (tags.size - index) * 50),
                        initialOffsetX = {
                            500
                        },
                    )
                    ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = 200 + (tags.size - index) * 50),
                ) + slideOutHorizontally(
                    animationSpec = tween(durationMillis = 200 + (tags.size - index) * 50),
                    targetOffsetX = {
                        500
                    },
                ),
            ) {
                Chip(
                    onClick = {
                    },
                ) {
                    Text(text = tag.tagValue)
                }
            }
        }
        Chip(
            onClick = onClickEdit,
        ) {
            Box {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isLoaded,
                ) {
                    Text(text = "#", fontSize = 24.sp)
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isLoaded,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        modifier = Modifier
                            .size(24.dp),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
