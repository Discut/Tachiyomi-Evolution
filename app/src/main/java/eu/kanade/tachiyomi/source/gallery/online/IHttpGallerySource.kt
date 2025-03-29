package eu.kanade.tachiyomi.source.gallery.online

import eu.kanade.tachiyomi.source.gallery.GallerySource
import eu.kanade.tachiyomi.source.gallery.model.SImage
import okhttp3.Response

interface IHttpGallerySource : GallerySource {

    suspend fun getImage(image: SImage): Response =
        getImage(image.url)

    suspend fun getImage(url: String): Response

    suspend fun getImageUrl(image: SImage): String
}
