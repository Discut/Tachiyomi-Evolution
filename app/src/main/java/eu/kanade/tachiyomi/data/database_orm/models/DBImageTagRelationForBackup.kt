package eu.kanade.tachiyomi.data.database_orm.models

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

@Serializable
data class DBImageTagRelationForBackup(
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "tag_value")
    val tagValue: String?,
)
