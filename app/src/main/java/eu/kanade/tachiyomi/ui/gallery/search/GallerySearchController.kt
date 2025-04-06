package eu.kanade.tachiyomi.ui.gallery.search

import android.view.LayoutInflater
import eu.kanade.tachiyomi.databinding.GallerySearchControllerBinding
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.ui.base.controller.BaseController

class GallerySearchController() : BaseController<GallerySearchControllerBinding>() {

    var tag: TagBo? = null

    override fun createBinding(inflater: LayoutInflater): GallerySearchControllerBinding =
        GallerySearchControllerBinding.inflate(inflater)

    override fun getTitle(): String? {
        return "Search"
    }

    fun showMiniBar() {
        /*binding.headerCard.isVisible = showCategoryInTitle
        setSubtitle()*/
    }
}
