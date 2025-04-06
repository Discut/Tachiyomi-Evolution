package eu.kanade.tachiyomi.source.gallery.model

import eu.kanade.tachiyomi.data.database_orm.models.DBImage
import eu.kanade.tachiyomi.util.toDate

open class SImage(
    val index: Int,
    val id: Long,
    val createdAt: String = "",
    val modifiedAt: String = "",
    val fileSize: Long = 0,
    val name: String = "",
    val url: String = "",
    val width: Int,
    val height: Int,
    val source: Long,
) : Loader() {
    val number: Int
        get() = index + 1
}

fun SImage.toDBImage() =
    DBImage(
        id = this.id,
        filePath = url,
        collectionPath = "/",
        fileSize = fileSize,
        exifJson = "",
        createdAt = createdAt.toDate(),
        modifiedAt = modifiedAt.toDate(),
        width = width,
        height = height,
        source = source,
    )
