package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.gallery.BackupResult
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.data.orm.GalleryDatabase
import eu.kanade.tachiyomi.ui.setting.gallery.AiTagFilterDialog
import eu.kanade.tachiyomi.ui.setting.gallery.GalleryDirSettingsDialog
import eu.kanade.tachiyomi.ui.setting.gallery.RecoveryGalleryDataDialog
import eu.kanade.tachiyomi.util.cancelOldJob
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

class SettingsGalleryController : SettingsController() {

    private var _dirFlow: MutableStateFlow<Dir> = MutableStateFlow(EMPTY_DIR)

    private val galleryManager: GalleryManager by injectLazy()
    private val galleryDatabase: GalleryDatabase by injectLazy()

    private var backupJob: Job? = null
    private var backupParesJob: Job? = null

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.gallery
        preferenceCategory {
            titleRes = R.string.general

            preference {
                key = "gallery_directory"
                titleRes = R.string.rescan_gallery
                summary = "Loading..."
                onClick {
                    activity?.toast("Rescanning images...")
                    galleryManager.syncImages()
                }

                viewScope.launchIO {
                    val countImages = galleryManager.countImages()
                    summary =
                        activity?.getString(R.string.rescan_gallery_summary, countImages.toString())
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.gallery_local_directory

            preference {
                key = "gallery_include_directory"
                titleRes = R.string.include_directory
                summaryRes = R.string.include_directory_summary
                onClick {
                    val context = activity
                    (context as? AppCompatActivity)?.apply {
                        val galleryScanIncludes = preferences.getGalleryScanIncludes()
                        val curtime = System.currentTimeMillis()
                        GalleryDirSettingsDialog(
                            context,
                            title = R.string.include_directory,
                            dirsFlow = galleryScanIncludes.asFlow(),
                            onDelete = {
                                galleryScanIncludes.set(galleryScanIncludes.get() - it)
                            },
                            onAdd = {
                                galleryScanIncludes.set(galleryScanIncludes.get() + it)
                            },
                            choiceDir = {
                                customDirectorySelected(INCLUDE_DIR)
                                getDirFlow().filter {
                                    it.type == INCLUDE_DIR && it.key > curtime
                                }.map { it.path }
                            },
                            onDismiss = {
                                cancelDirFlow()
                            },
                        ).apply {
                            show(context.supportFragmentManager, "galleryScanIncludes")
                        }
                    }
                }
            }

            preference {
                key = "gallery_exclude_directory"
                titleRes = R.string.exclude_directory
                summaryRes = R.string.exclude_directory_summary
                onClick {
                    val context = activity
                    (context as? AppCompatActivity)?.apply {
                        val galleryScanExcludes = preferences.getGalleryScanExcludes()
                        val curtime = System.currentTimeMillis()
                        GalleryDirSettingsDialog(
                            context,
                            title = R.string.exclude_directory,
                            dirsFlow = galleryScanExcludes.asFlow(),
                            onDelete = {
                                galleryScanExcludes.set(galleryScanExcludes.get() - it)
                            },
                            onAdd = {
                                galleryScanExcludes.set(galleryScanExcludes.get() + it)
                            },
                            choiceDir = {
                                customDirectorySelected(EXCLUDE_DIR)
                                getDirFlow().filter { it.type == EXCLUDE_DIR && it.key > curtime }.map { it.path }
                            },
                            onDismiss = {
                                cancelDirFlow()
                            },
                        ).apply {
                            show(context.supportFragmentManager, "galleryScanIncludes")
                        }
                    }
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.tag_settings

            preference {
                key = "gallery_tag_filter"
                titleRes = R.string.ai_tag_filter_settings
                summaryRes = R.string.ai_tag_filter_summary

                onClick {
                    showAITagFilterDialog()
                }
            }

            // Flow-driven summary update
            viewScope.launch {
                galleryDatabase.getTagFilterDao().getAllAsFlow().collect { list ->
                    findPreference<Preference>("gallery_tag_filter")?.summary =
                        activity?.getString(R.string.ai_tag_filter_summary, list.size)
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.backup

            preference {
                key = "gallery_backup_directory"
                titleRes = R.string.create_backup
                summaryRes = R.string.gallery_can_be_used_to_restore
                onClick {
                    backup()
                }
            }
            preference {
                key = "gallery_restore_directory"
                titleRes = R.string.restore_backup
                summaryRes = R.string.restore_from_backup_file
                onClick {
                    pickBackupFile()
                }
            }
        }
    }

    private fun showAITagFilterDialog() {
        val context = activity ?: return
        (context as? AppCompatActivity)?.apply {
            AiTagFilterDialog(
                dao = galleryDatabase.getTagFilterDao(),
            ).apply {
                show(supportFragmentManager, "aiTagFilter")
            }
        }
    }

    private fun customDirectorySelected(code: Int = INCLUDE_DIR) {
        val intent = if (code != PICK_FILE) {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }
        startActivityForResult(intent, code)
    }

    companion object {
        const val INCLUDE_DIR = 1024
        const val EXCLUDE_DIR = 1025
        const val RESTORE_DIR = 1026
        const val PICK_FILE = 1027

        data class Dir(val type: Int, val path: String, val key: Long)

        val EMPTY_DIR = Dir(1, "", 0L)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            INCLUDE_DIR, EXCLUDE_DIR, RESTORE_DIR, PICK_FILE -> if (data != null && resultCode == Activity.RESULT_OK) {
                val context = applicationContext ?: return
                val uri = data.data
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                if (uri != null && requestCode != PICK_FILE) {
                    @Suppress("NewApi")
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                }

                val file = UniFile.fromUri(context, uri)
                file.filePath?.let {
                    _dirFlow.value = Dir(
                        path = if (requestCode == PICK_FILE) getPathFromUri(context, uri!!) ?: it else it,
                        type = requestCode,
                        key = System.currentTimeMillis(),
                    )
                }
            } else {
                cancelDirFlow()
            }
        }
    }

    private fun backup() {
        backupJob.cancelOldJob()
        backupJob = viewScope.launchIO {
            try {
                val curtime = System.currentTimeMillis()
                customDirectorySelected(RESTORE_DIR)
                getDirFlow().filter { it.type == RESTORE_DIR && it.key > curtime }.collectLatest { dir ->
                    galleryManager.backupToDevice(dir.path)
                        .flowOn(Dispatchers.IO)
                        .collect { result ->
                            ensureActive()
                            when (result) {
                                is BackupResult.Error -> {
                                    withUIContext {
                                        activity?.toast(R.string.backup_failed)
                                    }
                                }

                                is BackupResult.Success -> {
                                    withUIContext {
                                        activity?.toast(
                                            activity?.getString(
                                                R.string.backup_created_and_info,
                                                result.msg,
                                            ),
                                        )
                                    }
                                }

                                is BackupResult.Progress, is BackupResult.Init -> {}
                            }
                        }
                }
            } catch (e: CancellationException) {
                Timber.e(e)
            }
        }
    }

    private fun pickBackupFile() {
        backupParesJob.cancelOldJob()
        backupParesJob = viewScope.launchIO {
            try {
                val curtime = System.currentTimeMillis()
                customDirectorySelected(PICK_FILE)
                getDirFlow().filter {
                    it.type == PICK_FILE && it.key > curtime
                }.collectLatest {
                    val context = activity
                    (context as? AppCompatActivity)?.apply {
                        ensureActive()
                        RecoveryGalleryDataDialog(
                            galleryManager.restoreFromDevice(it.path)
                                .flowOn(Dispatchers.IO),
                            onDismiss = {
                                cancelDirFlow()
                            },
                        ).apply {
                            withUIContext {
                                show(context.supportFragmentManager, "galleryScanIncludes")
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                Timber.e(e)
            }
        }
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    it.getString(columnIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            uri.path // 回退方案
        } finally {
            cursor?.close()
        }
    }

    private fun cancelDirFlow() {
        _dirFlow.value = EMPTY_DIR
        _dirFlow = MutableStateFlow(EMPTY_DIR)
    }

    private fun getDirFlow(): StateFlow<Dir> {
        return _dirFlow.asStateFlow()
    }
}
