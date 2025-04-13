package eu.kanade.tachiyomi.data.database_orm.models
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
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.kanade.tachiyomi.data.database_orm.requireValue

@Entity(
    tableName = "tag_types",
    // 复合约束配置
    // check = "type_id BETWEEN 1 AND 3",  // ID范围约束
    indices = [Index(value = ["type_name"], unique = true)], // 类型名称唯一
)
data class DBTagType(
    @PrimaryKey
    @ColumnInfo(name = "type_id")
    val typeId: Long, // 使用 Int 类型更符合取值范围约束

    @ColumnInfo(
        name = "type_name",
        collate = ColumnInfo.NOCASE, // 不区分大小写的唯一性
    )
    val typeName: String,
) {
    // 业务层二次校验
    init {
        require(typeId in 1..3) { "Invalid type ID: $typeId" }
        requireValue(typeName.isNotBlank()) { "类型名称不能为空" }
    }

    // 预置类型枚举
    enum class PresetType(val id: Int, val label: String) {
        AUTHOR(1, "author"),
        SOURCE(2, "source"),
        OTHER(3, "other"),
    }
}
