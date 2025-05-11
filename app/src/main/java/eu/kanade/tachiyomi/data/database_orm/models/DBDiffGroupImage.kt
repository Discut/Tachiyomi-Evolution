package eu.kanade.tachiyomi.data.database_orm.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverter
import eu.kanade.tachiyomi.data.database.tables.DiffGroupImageTable
import eu.kanade.tachiyomi.data.database.tables.DiffGroupTable
import eu.kanade.tachiyomi.data.database.tables.ImageTable
import java.math.BigDecimal

@Entity(
    tableName = DiffGroupImageTable.TABLE,
    primaryKeys = [DiffGroupImageTable.GROUP_ID, DiffGroupImageTable.IMAGE_ID],
    foreignKeys = [
        ForeignKey(
            entity = DBDiffGroup::class,
            parentColumns = [DiffGroupTable.GROUP_ID],
            childColumns = [DiffGroupImageTable.GROUP_ID],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DBImage::class,
            parentColumns = [ImageTable.ID],
            childColumns = [DiffGroupImageTable.IMAGE_ID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = [DiffGroupImageTable.SORT_ORDER], name = "idx_diff_group_order"),
        Index(
            value = [DiffGroupImageTable.GROUP_ID, DiffGroupImageTable.SORT_ORDER],
            name = "idx_diff_group_sort",
        ),
        Index(value = [DiffGroupImageTable.IMAGE_ID], name = "idx_diff_group_image_id"),
    ],
)
data class DBDiffGroupImage(
    @ColumnInfo(name = DiffGroupImageTable.GROUP_ID)
    val groupId: String,

    @ColumnInfo(name = DiffGroupImageTable.IMAGE_ID)
    val imageId: Long,

    @ColumnInfo(name = DiffGroupImageTable.SORT_ORDER, defaultValue = "1000")
    val sortOrder: BigDecimal,
) {
    // Room需要无参构造函数，可以添加@Ignore注解的辅助构造函数
    constructor() : this("", 0, BigDecimal.ZERO)
}

// 类型转换器（需要添加到Database配置）
class BigDecimalConverter {
    @TypeConverter
    fun fromString(value: String?): BigDecimal? = value?.toBigDecimal()

    @TypeConverter
    fun toString(bigDecimal: BigDecimal?): String? = bigDecimal?.toPlainString()
}
