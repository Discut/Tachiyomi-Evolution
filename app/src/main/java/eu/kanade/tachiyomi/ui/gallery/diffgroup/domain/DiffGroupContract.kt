package eu.kanade.tachiyomi.ui.gallery.diffgroup.domain

import eu.kanade.tachiyomi.data.orm.models.DBDiffGroup
import eu.kanade.tachiyomi.ui.base.viewmodel.UiEffect
import eu.kanade.tachiyomi.ui.base.viewmodel.UiEvent
import eu.kanade.tachiyomi.ui.base.viewmodel.UiState

sealed class DiffGroupState : UiState {
    data object Loading : DiffGroupState()

    data class Content(
        val isLoading: Boolean = false,
        val diffGroup: DBDiffGroup,
        val images: List<DiffImageVo>,
    ) : DiffGroupState()

    data class Error(
        val message: String,
        val error: Throwable? = null,
    ) : DiffGroupState()
}

sealed interface DiffGroupEvent : UiEvent {
    data class Init(val diffGroupId: String) : DiffGroupEvent
    data class ChangeOrder(val changed: List<DiffImageVo>, val original: List<DiffImageVo>) : DiffGroupEvent
}

sealed interface DiffGroupEffect : UiEffect
