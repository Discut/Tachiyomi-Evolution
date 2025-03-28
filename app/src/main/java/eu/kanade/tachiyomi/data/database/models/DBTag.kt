package eu.kanade.tachiyomi.data.database.models

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
class DBTag {
    var tagId: Long = 0
    var typeId: Long = 0
    var tagValue: String = ""
}
