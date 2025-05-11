package eu.kanade.tachiyomi.data.gallery

import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import uy.kohesive.injekt.injectLazy

object GalleryExtensions {

    private val galleryManager by injectLazy<GalleryManager>()

    suspend fun ReaderPage?.tryGetImage(): IImageBo? {
        if (this == null) return null
        if (id == null) return null
        return galleryManager.getImageBo(id!!)
    }
}
