package eu.kanade.tachiyomi.source.gallery.model

import eu.kanade.tachiyomi.data.database.models.DBImage

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
) : Loader() {
    val number: Int
        get() = index + 1
}

fun SImage.toDBImage() = DBImage().also {
    it.id = id
    it.filePath = url
    it.collectionPath = "/"
    it.fileSize = fileSize
    it.exifJson = ""
    it.createdAt = createdAt
    it.modifiedAt = modifiedAt
    it.width = width
    it.height = height
}
