package eu.kanade.tachiyomi.model

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.source.model.UpdateStrategy

class GalleryBo : Manga {

    companion object {
        val EMPTY = GalleryBo()
    }

    val manga = MangaImpl()

    var images = listOf<ImageBO>()

    var curImage: ImageBO? = null

    override var id: Long? = -1
    override var source: Long
        get() = curImage?.let { it.dbImage?.source } ?: -1
        set(value) {}

    @Deprecated("Use `favorite` instead")
    override var favorite: Boolean = false
    override var last_update: Long = -1
    override var date_added: Long = -1
    override var viewer_flags: Int = manga.viewer_flags
    override var chapter_flags: Int = manga.chapter_flags
    override var hide_title: Boolean = false
    override var filtered_scanlators: String? = manga.filtered_scanlators
    override var url: String = "GalleryId:$id"
    override var title: String = "GalleryId:$id"
    override var artist: String? = manga.artist
    override var author: String? = manga.author
    override var description: String? = manga.description
    override var genre: String? = manga.genre
    override var status: Int = manga.status
    override var thumbnail_url: String? = manga.thumbnail_url
    override var update_strategy: UpdateStrategy = manga.update_strategy
    override var initialized: Boolean = manga.initialized
}
