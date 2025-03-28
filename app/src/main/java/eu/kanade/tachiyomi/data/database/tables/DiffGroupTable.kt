package eu.kanade.tachiyomi.data.database.tables

/*

-- 差分组表（逻辑分组）
CREATE TABLE IF NOT EXISTS diff_groups
(
    group_id       TEXT    NOT NULL PRIMARY KEY CHECK (length(group_id) = 36), -- UUIDv4
    group_name     TEXT    NOT NULL CHECK (length(group_name) >= 2),
    cover_image_id INTEGER REFERENCES images (id) ON DELETE SET NULL,          -- 可空封面
    created_at     DATETIME DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))
);
 */
object DiffGroupTable {

    const val TABLE = "diff_groups"

    const val GROUP_ID = "group_id"

    const val GROUP_NAME = "group_name"

    const val COVER_IMAGE_ID = "cover_image_id"

    const val CREATED_AT = "created_at"

    val createTableQuery: String
        get() =
            """CREATE TABLE IF NOT EXISTS $TABLE(
            $GROUP_ID TEXT NOT NULL PRIMARY KEY CHECK (length($GROUP_ID) = 36),
            $GROUP_NAME TEXT NOT NULL CHECK (length($GROUP_NAME) >= 2),
            $COVER_IMAGE_ID INTEGER REFERENCES images (id) ON DELETE SET NULL,
            $CREATED_AT DATETIME DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))
            )"""

    val createIndexSql: String
        get() =
            """
                -- 索引优化（根据查询模式设计）
                CREATE INDEX IF NOT EXISTS idx_diff_group_name ON diff_groups ($GROUP_NAME);
            """
}
