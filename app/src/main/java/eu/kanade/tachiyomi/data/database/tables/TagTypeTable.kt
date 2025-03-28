package eu.kanade.tachiyomi.data.database.tables
/*
-- 标签类型字典表（预置作者/来源/其他分类）
CREATE TABLE IF NOT EXISTS tag_types
(
    type_id   INTEGER NOT NULL PRIMARY KEY CHECK (type_id BETWEEN 1 AND 3),
    type_name TEXT    NOT NULL UNIQUE
);
INSERT OR IGNORE INTO tag_types
VALUES (1, 'author'),
       (2, 'source'),
       (3, 'other');
 */
object TagTypeTable {

    const val TABLE = "tag_types"

    const val TYPE_ID = "type_id"

    const val TYPE_NAME = "type_name"

    val createTableQuery: String
        get() =
            """CREATE TABLE IF NOT EXISTS $TABLE(
            $TYPE_ID INTEGER NOT NULL PRIMARY KEY CHECK ($TYPE_ID BETWEEN 1 AND 3),
            $TYPE_NAME TEXT NOT NULL UNIQUE
            )"""
}
