package eu.kanade.tachiyomi.data.orm.models

import androidx.room.TypeConverter
import java.util.Date

// 时间类型转换器（必须）
class DateTimeConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
