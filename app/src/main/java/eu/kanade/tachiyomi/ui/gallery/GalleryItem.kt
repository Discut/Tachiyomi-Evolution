package eu.kanade.tachiyomi.ui.gallery

import eu.kanade.tachiyomi.model.ImageBO

sealed class GalleryItem {
    open var text: String = ""
    class Images(var images: List<ImageBO>, var height: Int) : GalleryItem()

    class Header(override var text: String) : GalleryItem()

    class AppBar(override var text: String) : GalleryItem()
}
