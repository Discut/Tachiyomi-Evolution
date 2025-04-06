package eu.kanade.tachiyomi.model

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.data.database_orm.GalleryDatabase
import eu.kanade.tachiyomi.data.database_orm.models.DBImage
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.text.SimpleDateFormat
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

    val createdTime: String by lazy {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dbImage.createdAt.let {
            formatter.format(it)
        }
    }

    val width: Int
        get() = dbImage.width

    val height: Int
        get() = dbImage.height

    val url = dbImage.filePath

    val source = dbImage.source

    // 预缓存，现主要用于排序
    val cachedTimeMillis: Long = dbImage.modifiedAt.time

    /**
     * get stream of bytes
     */
    var stream: (() -> InputStream)? = null

    suspend fun getTags(): List<TagBo> {
        Injekt.get<GalleryDatabase>().getImageDao().getImageWithTags(id)?.apply {
            return tags?.map { it.toTagBo() } ?: emptyList()
        }
        return emptyList()
    }
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
        date == today -> TODAY_TAG
        date == today.minusDays(1) -> YESTERDAY_TAG
        date.isAfter(today.minusDays(7)) -> WEEK_TAG + date.dayOfWeek.getDisplayName(
            TextStyle.FULL,
            Locale.getDefault(),
        )

        else -> "${date.year}年${date.monthValue.toString().padStart(2, '0')}月"
    }
}

const val TODAY_TAG = "9999"
const val YESTERDAY_TAG = "8888"
const val WEEK_TAG = "7777"
fun String.getRealDate(): String =
    when (this) {
        TODAY_TAG -> "今天"
        YESTERDAY_TAG -> "昨天"
        else -> this.replace(WEEK_TAG, "")
    }
