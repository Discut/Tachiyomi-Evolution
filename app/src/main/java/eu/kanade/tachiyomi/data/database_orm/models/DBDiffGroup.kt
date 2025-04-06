package eu.kanade.tachiyomi.data.database_orm.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

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
@Entity(
    tableName = "diff_groups",
)
@TypeConverters(DateTimeConverters::class)
data class DBDiffGroup(
    @PrimaryKey
    @ColumnInfo(
        name = "group_id",
        collate = ColumnInfo.NOCASE, // 优化UUID查询性能
        // check = "length(group_id) = 36"
    )
    val groupId: String, // UUIDv4

    @ColumnInfo(
        name = "group_name",
        // check = "length(group_name) >= 2"
    )
    val groupName: String,

    @ColumnInfo(name = "cover_image_id")
    val coverImageId: Int? = null, // 可空封面

    @ColumnInfo(
        name = "created_at",
        defaultValue = "CURRENT_TIMESTAMP",
    )
    val createdAt: Date = Date(), // 兼容Java/Kotlin的时间处理
) {
    init {
        // 业务层校验（双重保障）
        require(groupId.length == 36) { "Invalid UUIDv4 format" }
        require(groupName.length >= 2) { "Group name too short" }
    }
}
