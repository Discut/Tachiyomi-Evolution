package eu.kanade.tachiyomi.ui.setting.gallery

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.gallery.BackupResult
import eu.kanade.tachiyomi.theme.GalleryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * 恢复数据弹窗
 */
class RecoveryGalleryDataDialog(
    private val source: Flow<BackupResult>,
    private val onDismiss: () -> Unit,
) : DialogFragment() {
    @SuppressLint("ResourceType")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireContext().getString(R.string.backup_and_restore))
            .setView(
                ComposeView(requireContext()).apply {
                    setContent {
                        GalleryTheme {
                            val backupState by source.collectAsState(initial = BackupResult.Init, context = Dispatchers.IO)
                            Content(
                                backupState,
                            )
                        }
                    }
                },
            ).apply {
                setCancelable(false)
                setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss()
    }
}

@Composable
internal fun Content(
    state: BackupResult,
) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is BackupResult.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                BackupResult.Init -> {
                    Text(
                        text = "开始恢复数据",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                is BackupResult.Progress -> {
                    LinearProgressIndicator(
                        progress = { state.current.toFloat() / state.total.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }

                is BackupResult.Success -> {
                    Text(
                        text = state.msg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
