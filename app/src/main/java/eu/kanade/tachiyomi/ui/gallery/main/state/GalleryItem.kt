package eu.kanade.tachiyomi.ui.gallery.main.state

import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo

sealed class GalleryItem {
    open var text: String = ""
    class Images(var images: List<ImageBO>, var height: Int) : GalleryItem()

    class Header(override var text: String) : GalleryItem()

    class AppBar(
        override var text: String,
        val tags: List<TagVo> = emptyList(),
    ) : GalleryItem()
}
