package eu.kanade.tachiyomi.ui.gallery.tags

import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Random

class TagsPresenter(
    private val galleryManager: GalleryManager = Injekt.get(),
) : BaseCoroutinePresenter<TagsController>() {

    val tagsVoFlow: Flow<List<TagVo>> by lazy {
        galleryManager.getAllTagsVoAsFlow().map { it ->
            it.forEach {
                it.cover = if (it.images.isNotEmpty()) {
                    it.images.shuffled(Random(System.currentTimeMillis())).first()
                } else {
                    null
                }
            }
            it
        }
    }
}
