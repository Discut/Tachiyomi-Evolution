package eu.kanade.tachiyomi.data.orm.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.kanade.tachiyomi.data.database.tables.AnimationSequenceTable

/**
 * 动画序列实体
 *
 * 存储关键帧序列数据，用于图片的幻灯片动画效果
 *
 * @param id 序列唯一标识
 * @param imageId 关联的图片 ID（外键到 images.image_id）
 * @param name 序列名称（用户自定义）
 * @param durationMs 动画总时长（毫秒）
 * @param data 关键帧数据（JSON 格式字符串）
 * @param version 版本号，用于兼容性控制
 * @param exported 是否已导出
 * @param createdAt 创建时间（毫秒时间戳）
 * @param updatedAt 更新时间（毫秒时间戳）
 */
@Entity(
    tableName = AnimationSequenceTable.TABLE,
    indices = [
        Index(value = [AnimationSequenceTable.IMAGE_ID]),
        Index(value = [AnimationSequenceTable.EXPORTED]),
    ],
)
data class AnimationSequence(
    @PrimaryKey
    @ColumnInfo(name = AnimationSequenceTable.ID)
    val id: Long = 0,

    @ColumnInfo(
        name = AnimationSequenceTable.IMAGE_ID,
    )
    val imageId: Long,

    @ColumnInfo(
        name = AnimationSequenceTable.NAME,
    )
    val name: String,

    @ColumnInfo(
        name = AnimationSequenceTable.DURATION_MS,
    )
    val durationMs: Long,

    @ColumnInfo(
        name = AnimationSequenceTable.DATA,
    )
    val data: String,

    @ColumnInfo(
        name = AnimationSequenceTable.VERSION,
    )
    val version: Int = 1,

    @ColumnInfo(
        name = AnimationSequenceTable.EXPORTED,
    )
    val exported: Boolean = false,

    @ColumnInfo(
        name = AnimationSequenceTable.CREATED_AT,
    )
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(
        name = AnimationSequenceTable.UPDATED_AT,
    )
    val updatedAt: Long = System.currentTimeMillis(),
) {
    init {
        require(name.isNotBlank()) { "Name cannot be empty" }
        require(durationMs > 0) { "Duration must be positive" }
        require(data.isNotBlank()) { "Data cannot be empty" }
    }
}
