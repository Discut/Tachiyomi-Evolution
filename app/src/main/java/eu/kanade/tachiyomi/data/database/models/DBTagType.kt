package eu.kanade.tachiyomi.data.database.models
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
class DBTagType {
    var typeId: Long = 0
    var typeName: String = ""
}
