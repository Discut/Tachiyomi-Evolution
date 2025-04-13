package eu.kanade.tachiyomi.ui.setting.gallery

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.theme.GalleryTheme
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow

class GalleryDirSettingsDialog(
    private val activity: Activity,
    private val title: Int,
    private val dirsFlow: Flow<Set<String>>,
    private val onDelete: (String) -> Unit,
    private val onAdd: (String) -> Unit,
    private val choiceDir: () -> Flow<String>,
    private val onDismiss: () -> Unit = {},
) : DialogFragment() {

    val scope = MainScope()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return includeDirDialog(choiceDir).create()
    }

    private fun includeDirDialog(
        choiceDir: () -> Flow<String>,
    ): MaterialAlertDialogBuilder {
        activity.materialAlertDialog()
        return MaterialAlertDialogBuilder(activity).apply {
            setTitle(title)
            setView(
                ComposeView(activity).apply {
                    setContent {
                        val isDarkTheme = when (AppCompatDelegate.getDefaultNightMode()) {
                            AppCompatDelegate.MODE_NIGHT_YES -> true
                            AppCompatDelegate.MODE_NIGHT_NO -> false
                            else -> isSystemInDarkTheme() // You can define this function to check system theme preference if needed
                        }
                        GalleryTheme {
                            ProvideTextStyle(
                                value = LocalTextStyle.current.copy(
                                    color = if (isDarkTheme) Color.White else Color.Black,
                                ),
                            ) {
                                val dirs by dirsFlow.collectAsState(emptySet())
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    dirs.forEachIndexed { index, s ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "${index + 1}. $s",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .weight(1f) // 关键：文本占据剩余空间
                                                    .padding(end = 8.dp), // 添加右侧间距避免紧贴图标
                                            )

                                            IconButton(
                                                onClick = {
                                                    onDelete(s)
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.DeleteOutline,
                                                    contentDescription = "Delete",
                                                    tint = LocalTextStyle.current.color,
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launchUI {
                                                    choiceDir().collect {
                                                        onAdd(it)
                                                    }
                                                }
                                            }
                                            .padding(vertical = 8.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = "Add",
                                            tint = LocalTextStyle.current.color,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            )
            setNegativeButton(android.R.string.cancel, null)
            setOnDismissListener {
                onDismiss()
                scope.cancel()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss()
        scope.cancel()
    }
}
