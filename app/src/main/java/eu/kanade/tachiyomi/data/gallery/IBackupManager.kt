package eu.kanade.tachiyomi.data.gallery

import kotlinx.coroutines.flow.Flow

interface IBackupManager {
    fun backupToDevice(path: String): Flow<BackupResult>

    fun restoreFromDevice(path: String): Flow<BackupResult>
}

sealed class BackupResult {
    data object Init : BackupResult()
    data class Success(val msg: String) : BackupResult()
    data class Progress(val total: Int, val current: Int) : BackupResult()
    data class Error(val message: String) : BackupResult()
}
