package eu.kanade.tachiyomi.ui.gallery.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.UnionImageBO
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo
import eu.kanade.tachiyomi.util.system.toast

@Composable
fun GalleryEditBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    selectedImages: List<IImageBo>,
    tags: List<TagVo> = emptyList(),
    onDeleteImages: ((List<IImageBo>) -> Unit)? = null,
    onHideImages: ((List<IImageBo>) -> Unit)? = null,
    onShowImages: ((List<IImageBo>) -> Unit)? = null,
    onSplitImages: ((List<UnionImageBO>) -> Unit)? = null,
    onMergeImages: ((List<IImageBo>) -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    var isShowDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    ModalBottomSheet(
        modifier = Modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        dragHandle = {
            Surface(
                modifier =
                modifier
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
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OperationIconButton(
                    imageVector = Icons.Outlined.DeleteOutline,
                    label = stringResource(R.string.delete),
                    onClick = {
                        isShowDeleteDialog = true
                    },
                )

                if (selectedImages.any { !it.isHide }) {
                    OperationIconButton(
                        imageVector = Icons.Outlined.VisibilityOff,
                        label = stringResource(R.string.hide),
                        onClick = {
                            onHideImages?.invoke(selectedImages.filter { !it.isHide })
                        },
                    )
                } else {
                    OperationIconButton(
                        imageVector = Icons.Outlined.Visibility,
                        label = stringResource(R.string.show_all),
                        onClick = {
                            onShowImages?.invoke(selectedImages.filter { it.isHide })
                        },
                    )
                }

                OperationIconButton(
                    painter = painterResource(R.drawable.tab_group_24px),
                    label = stringResource(R.string.merge),
                    onClick = {
                        if (selectedImages.size < 2 ||
                            selectedImages.filterIsInstance<UnionImageBO>().size > 1 ||
                            !selectedImages.all { it.width == selectedImages.first().width && it.height == selectedImages.first().height }
                        ) {
                            context.toast(R.string.cannot_merge)
                            return@OperationIconButton
                        }
                        onMergeImages?.invoke(selectedImages)
                    },
                )

                OperationIconButton(
                    painter = painterResource(R.drawable.tab_close_24px),
                    label = stringResource(R.string.split),
                    onClick = {
                        if (selectedImages.size > 1 || selectedImages.first() !is UnionImageBO) {
                            context.toast(R.string.cannot_split)
                            return@OperationIconButton
                        }
                        onSplitImages?.invoke(selectedImages.filterIsInstance<UnionImageBO>())
                    },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = 4.dp,
                ),
            )
            if (tags.isNotEmpty()) {
                Text(
                    text = "添加到标签",
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = LocalTextStyle.current.color,
                )
                TagListCompose(
                    modifier = Modifier.padding(bottom = 16.dp),
                    tags = tags,
                )
            }
        }
    }

    if (isShowDeleteDialog) {
        DeleteImageAlertDialog(
            onDismissRequest = { isShowDeleteDialog = false },
            onConfirmation = {
                onDeleteImages?.invoke(selectedImages)
                isShowDeleteDialog = false
            },
        )
    }
}

@Composable
internal fun OperationIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    painter: Painter,
    iconColor: Color = LocalTextStyle.current.color,
    label: String? = null,
    labelColor: Color = iconColor,
) {
    Column(
        modifier = modifier
            .clickable {
                onClick()
            }
            .size(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            tint = iconColor,
        )
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                color = labelColor,
            )
        }
    }
}

@Composable
internal fun OperationIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    imageVector: ImageVector,
    iconColor: Color = LocalTextStyle.current.color,
    label: String? = null,
    labelColor: Color = iconColor,
) {
    OperationIconButton(
        modifier = modifier,
        onClick = onClick,
        painter = rememberVectorPainter(imageVector),
        iconColor = iconColor,
        label = label,
        labelColor = labelColor,
    )
}
