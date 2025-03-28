package eu.kanade.tachiyomi.model

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.data.database.models.DBImage
import eu.kanade.tachiyomi.source.gallery.model.SImage
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Immutable
class ImageBO(
    val sImage: SImage? = null,
    val dbImage: DBImage? = null,
) {

    val name: String
        get() = sImage?.name ?: dbImage?.filePath ?: ""

    val fileSize: Long
        get() = sImage?.fileSize ?: dbImage?.fileSize ?: 0

    val createdTime: String
        get() = sImage?.createdAt ?: dbImage?.createdAt ?: ""

    val width: Int
        get() = sImage?.width ?: dbImage?.width ?: 1

    val height: Int
        get() = sImage?.height ?: dbImage?.height ?: 1

    val url = sImage?.url ?: dbImage?.filePath ?: ""

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
