package eu.kanade.tachiyomi.data.database.tables
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
object ImageTable {

    const val TABLE = "images"

    const val ID = "id"

    const val FILE_PATH = "file_path"

    const val COLLECTION_PATH = "collection_path"

    const val FILE_SIZE = "filesize"

    const val CHECKSUM = "checksum"

    const val EXIF_JSON = "exif_json"

    const val CREATED_AT = "created_at"

    const val MODIFIED_AT = "modified_at"

    const val WIDTH = "width"

    const val HEIGHT = "height"

    val createTableQuery: String
        get() =
            """CREATE TABLE IF NOT EXISTS $TABLE(
            $ID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            $FILE_PATH TEXT NOT NULL UNIQUE CHECK (length($FILE_PATH) > 0),
            $COLLECTION_PATH TEXT NOT NULL CHECK (collection_path LIKE '/%'),
            $WIDTH INTEGER NOT NULL CHECK ($WIDTH > 0),
            $HEIGHT INTEGER NOT NULL CHECK ($HEIGHT > 0),
            $FILE_SIZE INTEGER NOT NULL CHECK ($FILE_SIZE > 0),
            $CHECKSUM TEXT CHECK (length($CHECKSUM) = 64),
            $EXIF_JSON TEXT,
            $CREATED_AT DATETIME NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')),
            $MODIFIED_AT DATETIME NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))
            )"""

    val createIndexSql: String
        get() =
            """
                -- 索引优化（根据查询模式设计）
                CREATE INDEX IF NOT EXISTS idx_image_path ON images ($FILE_PATH);
                CREATE INDEX IF NOT EXISTS idx_collection ON images ($COLLECTION_PATH);
                -- 更新时间戳触发器
                CREATE TRIGGER IF NOT EXISTS update_image_timestamp
                    AFTER UPDATE
                    ON $TABLE
                    FOR EACH ROW
                BEGIN
                    UPDATE images
                    SET $MODIFIED_AT = STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')
                    WHERE $ID = OLD.$ID;
                END;
            """
}
