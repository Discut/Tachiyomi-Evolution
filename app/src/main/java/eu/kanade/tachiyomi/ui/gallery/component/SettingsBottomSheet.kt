package eu.kanade.tachiyomi.ui.gallery.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    isShowHideImage: Boolean,
    isShowDiffGroup: Boolean,
    imageRowSize: Int,
    onClickHideImage: (Boolean) -> Unit,
    onClickShowDiffGroup: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onImageRowSizeChange: (Int) -> Unit,
) {
    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        dragHandle = {
            Surface(
                modifier = modifier
                    .padding(vertical = 8.dp)
                    .semantics {
                        contentDescription = ""
                    },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Box(Modifier.size(width = 32.dp, height = 4.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "图片显示",
                style = MaterialTheme.typography.titleLarge,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("每行最大数目", modifier = Modifier.padding(bottom = 4.dp))
                    Text(
                        modifier = Modifier
                            .wrapContentSize(Alignment.Center),
                        text = imageRowSize.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = imageRowSize.toFloat(),
                    valueRange = 5f..10f,
                    steps = 4,
                    onValueChange = {
                        onImageRowSizeChange(it.toInt())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
            Row(
                modifier = Modifier
                    .clickable {
                        onClickHideImage(!isShowHideImage)
                    }
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isShowHideImage,
                    onCheckedChange = {},
                )
                Text(
                    "展示隐藏的图片",
                )
            }
            Row(
                modifier = Modifier
                    .clickable {
                        onClickShowDiffGroup(!isShowDiffGroup)
                    }
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isShowDiffGroup,
                    onCheckedChange = { },
                )
                Text(
                    "展示差分图",
                )
            }
        }
    }
}
