package eu.kanade.tachiyomi.data.orm.models

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

@Serializable
data class DBImageTagRelationForBackup(
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "tag_value")
    val tagValue: String?,
)

fun DBImageTagRelationForBackup.tagValueList(): List<String> = tagValue?.split(",") ?: emptyList()
