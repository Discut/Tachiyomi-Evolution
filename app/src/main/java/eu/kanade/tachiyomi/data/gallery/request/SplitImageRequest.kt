package eu.kanade.tachiyomi.data.gallery.request

import eu.kanade.tachiyomi.model.UnionImageBO

data class SplitImageRequest(
    val targets: List<UnionImageBO>,
) {
    init {
        require(targets.isNotEmpty()) {
            "targets must not be empty"
        }
    }
}
