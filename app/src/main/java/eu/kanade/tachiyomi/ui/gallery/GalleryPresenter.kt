package eu.kanade.tachiyomi.ui.gallery

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.getDateTimeTag
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.stream.Collectors

class GalleryPresenter(
    val db: DatabaseHelper = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val galleryManager: GalleryManager = Injekt.get(),
) : BaseCoroutinePresenter<GalleryController>() {

    val sourceImage: MutableStateFlow<List<ImageBO>> by lazy {
        galleryManager.sourceImage
    }

    var sourceImageSortDate: MutableStateFlow<Map<String, List<ImageBO>>> =
        MutableStateFlow(
            emptyMap(),
        )

    override fun onCreate() {
        super.onCreate()

        presenterScope.launchIO {
            sourceImage.collect {
                sourceImageSortDate.value = it.parallelStream()
                    .collect(
                        Collectors.groupingBy {
                            it.getDateTimeTag()
                        },
                    ).toMap()
                    .toSortedMap { a, b -> b.compareTo(a) }
            }
        }
    }
}
