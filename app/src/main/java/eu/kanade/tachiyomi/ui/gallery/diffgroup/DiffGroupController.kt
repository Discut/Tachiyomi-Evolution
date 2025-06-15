package eu.kanade.tachiyomi.ui.gallery.diffgroup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ComposeControllerBinding
import eu.kanade.tachiyomi.theme.GalleryTheme
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.gallery.diffgroup.domain.DiffGroupEvent
import eu.kanade.tachiyomi.ui.gallery.diffgroup.domain.DiffGroupState

class DiffGroupController(
    bundle: Bundle? = null,
) : BaseCoroutineController<ComposeControllerBinding, DiffGroupViewModel>(bundle) {

    override val presenter = DiffGroupViewModel()

    override fun createBinding(inflater: LayoutInflater): ComposeControllerBinding {
        return ComposeControllerBinding.inflate(inflater)
    }

    companion object {
        const val TAG = "DiffGroupController"
        const val KEY_DIFF_GROUP_ID = "diffGroupId"
    }

    override fun getTitle(): String? = null

    override fun getSearchTitle(): String {
        return "search"
    }

    constructor(diffGroupId: String) : this(
        Bundle().apply {
            putString(KEY_DIFF_GROUP_ID, diffGroupId)
        },
    )

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        activity?.findViewById<View>(R.id.app_bar)?.visibility = View.GONE
        val diffGroupId = args.getString(KEY_DIFF_GROUP_ID) ?: ""
        binding.root.setContent {
            val isDarkTheme = when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> isSystemInDarkTheme() // You can define this function to check system theme preference if needed
            }
            GalleryTheme {
                ProvideTextStyle(
                    value = LocalTextStyle.current.copy(
                        color = if (isDarkTheme) Color.White else Color.Black,
                    ),
                ) {
                    val state by presenter.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(key1 = diffGroupId) {
                        presenter.sendEvent {
                            DiffGroupEvent.Init(diffGroupId)
                        }
                    }

                    BackHandler {
                        router.popCurrentController()
                    }
                    DiffGroupScreen(
                        state = state,
                        onChangedImageOrder = {
                            if (state is DiffGroupState.Content) {
                                presenter.sendEvent {
                                    DiffGroupEvent.ChangeOrder(changed = it, original = (state as DiffGroupState.Content).images)
                                }
                            }
                        },
                        onBack = {
                            router.popCurrentController()
                        },
                    )
                }
            }
        }
    }
}
