package eu.kanade.tachiyomi.model

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.data.orm.GalleryDatabase
import eu.kanade.tachiyomi.data.orm.models.DBImage
import eu.kanade.tachiyomi.source.gallery.local.LocalGallerySource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

interface IImageBo {
    val aspectRatio: Float
    val id: Long
    val name: String
    val fileSize: Long
    val url: String
    val source: Long
    val createdTime: String
    val modifiedAt: Date
    val width: Int
    val height: Int
    val order: BigDecimal
    val isHide: Boolean

    // 预缓存，现主要用于排序
    val cachedTimeMillis: Long
}

@Immutable
class ImageBO(
    val dbImage: DBImage,
    override val order: BigDecimal = BigDecimal.ZERO,
) : IImageBo {

    override val aspectRatio: Float by lazy {
        if (height == 0) 1f else width.toFloat() / height
    }

    override val id = dbImage.id

    override val name: String
        get() = dbImage.filePath

    override val fileSize: Long
        get() = dbImage.fileSize

    override val createdTime: String by lazy {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dbImage.createdAt.let {
            formatter.format(it)
        }
    }

    override val modifiedAt: Date
        get() = dbImage.modifiedAt

    override val width: Int
        get() = dbImage.width

    override val height: Int
        get() = dbImage.height

    override val isHide: Boolean
        get() = dbImage.isHide
    override val cachedTimeMillis: Long
        get() = modifiedAt.time

    override val url = dbImage.filePath

    override val source = dbImage.source

    /**
     * get stream of bytes
     */
    /*    var stream: (() -> InputStream)? = null*/
}

class UnionImageBO(
    val unions: List<ImageBO>,
    val diffGroupId: String,
) : IImageBo {

    init {
        require(unions.isNotEmpty()) {
            "unions must not be empty"
        }
    }

    override val aspectRatio: Float
        get() = unions.first().aspectRatio
    override val id: Long
        get() = unions.first().id
    override val name: String
        get() = unions.first().name
    override val fileSize: Long
        get() = unions.sumOf { it.fileSize }
    override val url: String
        get() = unions.first().url
    override val source: Long
        get() = unions.first().source
    override val createdTime: String
        get() = unions.first().createdTime
    override val width: Int
        get() = unions.first().width
    override val height: Int
        get() = unions.first().height
    override val order: BigDecimal
        get() = BigDecimal.ZERO
    override val isHide: Boolean
        get() = unions.all { it.isHide }
    override val cachedTimeMillis: Long
        get() = unions.first().cachedTimeMillis
    override val modifiedAt: Date
        get() = unions.first().modifiedAt
}

fun IImageBo.getDateTimeTag(): String {
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
fun DBImage.toImageBO(hasThumbnail: Boolean = false, order: BigDecimal = BigDecimal.ZERO): ImageBO =
    ImageBO(this, order)

fun IImageBo.isFromLocalSource(): Boolean = id == LocalGallerySource.ID

suspend fun IImageBo.getTags(): List<TagBo> {
    Injekt.get<GalleryDatabase>().getImageDao().getImageWithTags(id)?.apply {
        return tags?.map { it.toTagBo() } ?: emptyList()
    }
    return emptyList()
}
