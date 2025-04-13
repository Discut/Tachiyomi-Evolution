package eu.kanade.tachiyomi.source.gallery.model

import java.io.InputStream

data class ImageStream(
    val originStream: InputStream,
    val width: Int,
    val height: Int,
) : AutoCloseable by originStream
