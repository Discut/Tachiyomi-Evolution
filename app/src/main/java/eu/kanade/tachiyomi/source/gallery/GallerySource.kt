package eu.kanade.tachiyomi.source.gallery

import eu.kanade.tachiyomi.source.gallery.model.ImageStream
import eu.kanade.tachiyomi.source.gallery.model.Page
import eu.kanade.tachiyomi.source.gallery.model.SImage
import eu.kanade.tachiyomi.source.gallery.model.STag

interface GallerySource {
    /**
     * ID for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    val lang: String
        get() = ""

    suspend fun getImageStream(image: SImage): ImageStream?

    suspend fun getThumbnailImageStream(image: SImage): ImageStream?

    suspend fun getAllImages(): List<SImage>

    suspend fun collectImages(pageIndex: Int = 0, pageSize: Int): Page<SImage>

    suspend fun getAllTags(): List<STag>

    suspend fun collectTags(pageIndex: Int = 0, pageSize: Int): Page<STag>

    suspend fun collectImagesByTag(tag: STag, pageIndex: Int = 0, pageSize: Int): Page<SImage>

    suspend fun getAllImagesByTag(tag: STag): List<SImage>
}
