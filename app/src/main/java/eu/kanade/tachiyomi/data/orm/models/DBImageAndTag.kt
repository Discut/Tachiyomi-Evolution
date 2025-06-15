package eu.kanade.tachiyomi.data.orm.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/*
-- 图片-标签关联表（多对多关系）
CREATE TABLE IF NOT EXISTS image_tags
(
    image_id INTEGER NOT NULL REFERENCES images (id) ON DELETE CASCADE,
    tag_id   INTEGER NOT NULL REFERENCES tags (tag_id) ON DELETE CASCADE,
    PRIMARY KEY (image_id, tag_id)
);
 */
@Entity(
    tableName = "image_tags",
    primaryKeys = ["image_id", "tag_id"], // 联合主键
    foreignKeys = [
        ForeignKey(
            entity = DBImage::class,
            parentColumns = ["image_id"],
            childColumns = ["image_id"],
            // onDelete = ForeignKey.CASCADE, // 级联删除
        ),
        ForeignKey(
            entity = DBTag::class,
            parentColumns = ["tag_id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tag_id"), // 为 tag_id 添加索引
        Index("image_id"), // 为 image_id 添加索引
    ],
)
data class DBImageAndTag(
    @ColumnInfo(name = "image_id")
    val imageId: Long,

    @ColumnInfo(name = "tag_id")
    val tagId: Long,
) {
    // 业务层校验（可选）
    init {
        require(imageId > 0) { "Invalid image ID" }
        require(tagId > 0) { "Invalid tag ID" }
    }
}
