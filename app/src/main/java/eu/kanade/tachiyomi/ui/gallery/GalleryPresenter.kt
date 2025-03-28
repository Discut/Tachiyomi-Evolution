package eu.kanade.tachiyomi.ui.gallery

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.getDateTimeTag
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.model.toDBImage
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

class GalleryPresenter(
    val db: DatabaseHelper = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
) : BaseCoroutinePresenter<GalleryController>() {

    private var sourceImage: MutableStateFlow<List<ImageBO>> = MutableStateFlow(emptyList())

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

    fun fetchAllImages(): Flow<List<ImageBO>> {
        presenterScope.launchIO {
            val localSource = sourceManager.getGallerySource()
            if (sourceImage.value.isNotEmpty()) {
                return@launchIO
            }

            var localImages = db.getAllImages().executeAsBlocking().map { ImageBO(dbImage = it) }

            if (localImages.isEmpty()) {
                val allImages = localSource?.getAllImages()
                allImages?.map { it.toDBImage() }?.let {
                    db.insertImages(it).executeAsBlocking()
                }
                localImages = allImages?.map { ImageBO(sImage = it) } ?: emptyList()
            }

            val allImages = localImages // localSource?.collectImages(1, 50) ?: Page<SImage>(0, 0, 0, emptyList())
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            sourceImage.value = allImages
                .sortedBy {
                    try {
                        LocalDateTime.parse(it.createdTime, formatter)
                    } catch (e: Exception) {
                        LocalDateTime.MIN
                    }
                }
        }
        return sourceImage
    }
}
