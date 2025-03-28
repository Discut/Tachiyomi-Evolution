package eu.kanade.tachiyomi.data.database.models

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
class DBDiffGroup {
    var groupId: String = ""
    var groupName: String = ""
    var coverImageId: Long? = null
    var createdAt: String = ""
}
