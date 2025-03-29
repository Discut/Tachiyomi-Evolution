package eu.kanade.tachiyomi.model

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.data.database.models.DBImage
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Immutable
class ImageBO(
    val dbImage: DBImage,
) {

    val aspectRatio: Float by lazy {
        if (height == 0) 1f else width.toFloat() / height
    }

    val id = dbImage.id

    val name: String
        get() = dbImage.filePath

    val fileSize: Long
        get() = dbImage.fileSize

    val createdTime: String
        get() = dbImage.createdAt

    val width: Int
        get() = dbImage.width

    val height: Int
        get() = dbImage.height

    val url = dbImage.filePath

    val source = dbImage.source

    /**
     * get stream of bytes
     */
    var stream: (() -> InputStream)? = null
}

fun ImageBO.getDateTimeTag(): String {
    try {
        val parse = LocalDate.parse(createdTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return getDateLabel(parse)
    } catch (e: Exception) {
        return createdTime
    }
}

// 通过资源文件动态获取标签
private fun getDateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.isAfter(today.minusDays(7)) -> date.dayOfWeek.getDisplayName(
            TextStyle.FULL,
            Locale.getDefault(),
        )

        else -> "${date.year}年${date.monthValue.toString().padStart(2, '0')}月"
    }
}
