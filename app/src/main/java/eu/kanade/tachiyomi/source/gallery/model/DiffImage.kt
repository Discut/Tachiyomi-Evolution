package eu.kanade.tachiyomi.source.gallery.model

class DiffImage(
    index: Int,
    id: Long,
    createdAt: String = "",
    modifiedAt: String = "",
    fileSize: Long = 0,
    name: String = "",
    url: String = "",
    width: Int,
    height: Int,
) : SImage(index, id, createdAt, modifiedAt, fileSize, name, url, width, height)
