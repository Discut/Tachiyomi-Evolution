package eu.kanade.tachiyomi.data.database.tables

/*
-- 标签表（带类型约束）
CREATE TABLE IF NOT EXISTS tags
(
    tag_id    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    type_id   INTEGER NOT NULL REFERENCES tag_types (type_id),
    tag_value TEXT    NOT NULL CHECK (length(tag_value) >= 2),
    UNIQUE (type_id, tag_value) -- 防止重复标签
);
 */
object TagTable {

    const val TABLE = "tags"

    const val TAG_ID = "tag_id"

    const val TYPE_ID = "type_id"

    const val TAG_VALUE = "tag_value"

    val createTableQuery: String
        get() =
            """CREATE TABLE IF NOT EXISTS $TABLE(
            $TAG_ID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            $TYPE_ID INTEGER NOT NULL REFERENCES tag_types (type_id),
            $TAG_VALUE TEXT NOT NULL CHECK (length($TAG_VALUE) >= 2),
            UNIQUE ($TYPE_ID, $TAG_VALUE)
            )"""
    val createIndexSql: String
        get() =
            """
            CREATE INDEX IF NOT EXISTS idx_tag_search ON tags ($TAG_VALUE);
            """

    val createInitData: String
        get() =
            """
                INSERT OR IGNORE INTO tag_types
                VALUES (1, 'author'),
                       (2, 'source'),
                       (3, 'other');
            """
}
