package eu.kanade.tachiyomi.ui.gallery.main.state

import androidx.compose.runtime.Stable
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.ui.base.viewmodel.UiEffect
import eu.kanade.tachiyomi.ui.base.viewmodel.UiEvent
import eu.kanade.tachiyomi.ui.base.viewmodel.UiState
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo

sealed class GalleryMainState : UiState {
    @Stable
    data class Success(
        val isLoading: Boolean = false,
        val items: List<GalleryItem>,
        val tags: List<TagVo>,
    ) : GalleryMainState()

    data object Loading : GalleryMainState()

    @Stable
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : GalleryMainState()
}

sealed interface GalleryEffect : UiEffect {
    data class LaunchReader(val images: List<IImageBo>, val index: Int) : GalleryEffect

    data class ShowToast(val message: String) : GalleryEffect
}

sealed interface GalleryEvent : UiEvent {
    data class Load(val containerWidth: Int, val containerHeight: Int) :
        GalleryEvent

    data class SelectTag(val tag: TagVo) : GalleryEvent

    data object ClearAllSelectedTags : GalleryEvent

    data object RandomPlay : GalleryEvent
}
