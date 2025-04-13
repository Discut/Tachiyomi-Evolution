package eu.kanade.tachiyomi.ui.gallery.main.state

sealed class GalleryMainState {
    data object Loading : GalleryMainState()
    data object Error : GalleryMainState()
}
