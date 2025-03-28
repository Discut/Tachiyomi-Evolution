package eu.kanade.tachiyomi.ui.gallery

import eu.kanade.tachiyomi.model.ImageBO

sealed class GalleryItem {

    class Images(var images: List<ImageBO>, var height: Int) : GalleryItem()

    class Header(val text: String) : GalleryItem()
}
