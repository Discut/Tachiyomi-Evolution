package eu.kanade.tachiyomi.data.database_orm.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import eu.kanade.tachiyomi.data.database.tables.ImageTable
import java.util.Date

/*
-- 图片元数据表（核心实体）
CREATE TABLE IF NOT EXISTS images
(
    id              INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
    file_path       TEXT     NOT NULL UNIQUE CHECK (length(file_path) > 0), -- 文件绝对路径
    collection_path TEXT     NOT NULL CHECK (collection_path LIKE '/%'),    -- 合集路径（Linux风格）
    filesize        INTEGER  NOT NULL CHECK (filesize > 0),                 -- 文件大小（字节）
    checksum        TEXT CHECK (length(checksum) = 64),                     -- SHA256校验（可选）
    exif_json       TEXT,                                                   -- EXIF元数据（JSON格式）
    created_at      DATETIME NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')),
    modified_at     DATETIME NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))
);
 */

@Entity(
    tableName = ImageTable.TABLE,
    // 添加复合索引提升查询性能
    indices = [
        Index(value = ["collection_path"]),
        Index(value = ["file_path"], unique = true),
    ],
)
@TypeConverters(DateTimeConverters::class)
data class DBImage(
    @PrimaryKey
    @ColumnInfo(name = "image_id")
    val id: Long = 0,

    @ColumnInfo(
        name = "file_path",
        // 对齐 SQL 约束：非空 + 唯一 + 长度校验
        collate = ColumnInfo.NOCASE,
        // check = "length(file_path) > 0"
    )
    val filePath: String,

    @ColumnInfo(
        name = "collection_path",
        // 路径格式校验（Linux风格）
        // check = "collection_path LIKE '/%'"
    )
    val collectionPath: String,

    @ColumnInfo(
        name = "width",
    )
    val width: Int = 1,

    @ColumnInfo(
        name = "height",
    )
    val height: Int = 1,

    @ColumnInfo(
        name = "source",
    )
    val source: Long = 0,

    @ColumnInfo(
        name = "filesize",
        // 文件大小必须 >0
        // check = "filesize > 0"
    )
    val fileSize: Long,

    @ColumnInfo(
        name = "checksum",
        // SHA256 校验码约束（可选）
        // check = "checksum IS NULL OR length(checksum) = 64"
    )
    val checksum: String? = null,

    @ColumnInfo(name = "exif_json")
    val exifJson: String? = null,

    @ColumnInfo(
        name = "created_at",
        // 使用精确到毫秒的时间格式
        defaultValue = "CURRENT_TIMESTAMP",
    )
    val createdAt: Date = Date(),

    @ColumnInfo(
        name = "modified_at",
        // 更新时自动刷新时间
        defaultValue = "CURRENT_TIMESTAMP",
    )
    val modifiedAt: Date = Date(),

    @ColumnInfo(
        name = "is_hide",
    )
    val isHide: Boolean = false,
) {
    // 业务层二次校验（与数据库约束形成双重保障）
    init {
        require(filePath.isNotBlank()) { "File path cannot be empty" }
        require(collectionPath.startsWith("/")) { "Invalid collection path format" }
        require(fileSize > 0) { "File size must be positive" }
        checksum?.let { require(it.length == 64) { "Invalid SHA256 checksum" } }
    }
}
