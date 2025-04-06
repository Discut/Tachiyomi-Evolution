package eu.kanade.tachiyomi.ui.gallery

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.model.getDateTimeTag
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GalleryPresenter(
    val db: DatabaseHelper = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val galleryManager: GalleryManager = Injekt.get(),
    val targetTag: TagBo? = null,
) : BaseCoroutinePresenter<GalleryController>() {

    val sourceImage: Flow<List<ImageBO>> by lazy {
        (
            targetTag?.let { galleryManager.getAllImagesByTagIdAsFlow(it.tagId) }
                ?: galleryManager.sourceImage
            ).map {
            it.asSequence().sortedByDescending { it.cachedTimeMillis }.toList()
        }
    }

    var sourceImageSortDate: MutableStateFlow<Map<String, List<ImageBO>>> =
        MutableStateFlow(
            emptyMap(),
        )

    override fun onCreate() {
        super.onCreate()

        presenterScope.launchIO {
            sourceImage.collect { it ->
                sourceImageSortDate.value = it
                    .groupBy { it.getDateTimeTag() }
                    .toSortedMap(compareByDescending { it }) // 按日期降序排序
            }
        }
    }
}
