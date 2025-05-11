package eu.kanade.tachiyomi.data.gallery.request

import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.UnionImageBO
import java.util.Date

data class MergeImageRequest(
    val diffGroupName: String,
    val images: List<ImageBO>,
    val target: UnionImageBO? = null,
    val createDate: Date = Date(),
    val headerImage: ImageBO = images.first(),
) {
    init {
        require(images.size > 1 || images.size == 1 && target != null) {
            "MergeImageRequest must have at least 2 images"
        }
    }
}
