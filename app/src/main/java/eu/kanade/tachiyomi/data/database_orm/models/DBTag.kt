package eu.kanade.tachiyomi.data.database_orm.models

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
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.kanade.tachiyomi.data.database_orm.requireValue

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = DBTagType::class,
            parentColumns = ["type_id"],
            childColumns = ["type_id"],
            onDelete = ForeignKey.CASCADE, // 级联删除
        ),
    ],
    indices = [
        Index(
            value = ["type_id", "tag_value"],
            unique = true, // 联合唯一约束
        ),
    ],
)
data class DBTag(
    @PrimaryKey
    @ColumnInfo(name = "tag_id")
    val tagId: Long = 0,

    @ColumnInfo(name = "type_id")
    val typeId: Long,

    @ColumnInfo(
        name = "tag_value",
        // check = "length(tag_value) >= 2", // SQL层校验
    )
    val tagValue: String,

    @ColumnInfo(
        name = "source",
    )
    val source: Long,
) {
    // 业务层二次校验
    init {
        requireValue(tagValue.length >= 2) { "标签长度不能小于2" }
        require(typeId > 0) { "Invalid type ID" }
    }
}
