package eu.kanade.tachiyomi.ui.gallery.main.state

import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo

sealed class GalleryItem {
    open var text: String = ""
    class Images(var images: List<IImageBo>, var height: Int) : GalleryItem()

    class Header(override var text: String, val content: List<IImageBo> = emptyList()) : GalleryItem()

    class AppBar(
        override var text: String,
        val tags: List<TagVo> = emptyList(),
    ) : GalleryItem()
}
