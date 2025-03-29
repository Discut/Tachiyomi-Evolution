package eu.kanade.tachiyomi.data.database.models

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
class DBImage {
    var id: Long = 0
    var filePath: String = ""
    var collectionPath: String = ""
    var fileSize: Long = 0
    var width: Int = 1
    var height: Int = 1
    var checksum: String? = null
    var exifJson: String? = null
    var source: Long = 0
    var createdAt: String = ""
    var modifiedAt: String = ""
}
