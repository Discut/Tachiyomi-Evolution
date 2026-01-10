package eu.kanade.tachiyomi.ui.gallery.main

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.GalleryControllerBinding
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.theme.GalleryTheme
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.gallery.component.GalleryCompose
import eu.kanade.tachiyomi.ui.gallery.diffgroup.DiffGroupController
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryEffect
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryEvent
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryItem
import eu.kanade.tachiyomi.ui.gallery.main.state.GalleryMainState
import eu.kanade.tachiyomi.ui.gallery.tags.TagsController
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.sheet.TagVo
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.pxToDp
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.withFadeInTransaction
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.injectLazy

class GalleryController(
    bundle: Bundle? = null,
    private var targetTag: TagBo? = null,
) :
    BaseCoroutineController<GalleryControllerBinding, GalleryViewModel>(bundle),
    RootSearchInterface {

    private val galleryManager: GalleryManager by injectLazy()

    override val presenter by lazy {
        GalleryViewModel(targetTag = targetTag)
    }

    override fun createBinding(inflater: LayoutInflater): GalleryControllerBinding {
        return GalleryControllerBinding.inflate(inflater)
    }

    override fun getTitle(): String? = null

    override fun getSearchTitle(): String {
        return "search"
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        handleEffect()
        activity?.findViewById<View>(R.id.app_bar)?.visibility = View.GONE
        view.findViewById<ComposeView>(R.id.gallery_container)?.setContent {
            val isDarkTheme = when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> isSystemInDarkTheme() // You can define this function to check system theme preference if needed
            }
            val screenHeight = resources?.displayMetrics?.heightPixels

            GalleryTheme {
                ProvideTextStyle(
                    value = LocalTextStyle.current.copy(
                        color = if (isDarkTheme) Color.White else Color.Black,
                    ),
                ) {
                    val prefetchStrategy = remember {
                        LazyListPrefetchStrategy(
                            nestedPrefetchItemCount = 6,
                        )
                    }
                    val scrollState: LazyListState = rememberLazyListState(
                        prefetchStrategy = prefetchStrategy,
                    )
                    val state by presenter.uiState.collectAsState()
                    val imageBOList = presenter.sourceImage.collectAsState(emptyList()).value
                    val preference: PreferencesHelper by injectLazy()

                    when (state) {
                        is GalleryMainState.Error -> {
                        }

                        GalleryMainState.Loading,
                        is GalleryMainState.Success,
                        -> {
                            var tags: List<TagVo> = emptyList()
                            var items: List<GalleryItem> = emptyList()
                            (state as? GalleryMainState.Success)?.apply {
                                tags = this.tags
                                items = this.items
                            }
                            GalleryCompose(
                                paddingValues = PaddingValues(
                                    top = getStatusBarHeight(activity!!).pxToDp.dp,
                                    bottom = getNavigationBarHeight(activity!!).pxToDp.dp,
                                ),

                                isLoading = state is GalleryMainState.Loading || (state as? GalleryMainState.Success)?.isLoading == true,
                                screenHeight = screenHeight!!,
                                scrollState = scrollState,
                                tags = tags,
                                itemList = items,
                                isSelectedTags = tags.any { it.isSelected },
                                onSizeChanged = { wight ->
                                    sendEvent {
                                        GalleryEvent.Load(wight, screenHeight)
                                    }
                                },
                                onRandomPlay = outer@{
                                    sendEvent {
                                        GalleryEvent.RandomPlay
                                    }
                                },
                                onClearAllSelected = {
                                    sendEvent {
                                        GalleryEvent.ClearAllSelectedTags
                                    }
                                },
                                onClickJumpToTags = {
                                    /*router.setRoot(
                                        TagsController().withFadeInTransaction().tag(TagsController.ID),
                                    )*/
                                    router.pushController(
                                        TagsController().withFadeInTransaction()
                                            .tag(TagsController.ID),
                                    )
                                },
                                onClickTag = {
                                    presenter.sendEvent {
                                        GalleryEvent.SelectTag(it)
                                    }
                                },
                                onPlayGallery = { current, sources ->

                                    val galleryId =
                                        galleryManager.putTempGallery(sources.ifEmpty { imageBOList })
                                    // 图片点击处理
                                    ReaderActivity.newIntentToGallery(
                                        activity!!,
                                        galleryId,
                                        current.id,
                                    )
                                        .apply {
                                            startActivity(this)
                                        }
                                },
                                onChangeImagesVisible = { images, visible ->
                                    if (visible) {
                                        presenter.showImages(images)
                                    } else {
                                        presenter.hideImages(images)
                                    }
                                },
                                onDeleteImages = {
                                    presenter.deleteImages(it)
                                },
                                onSplitImages = {
                                    presenter.splitDiffGroup(it)
                                },
                                onMergeImages = {
                                    if (preference.isMergeDiffImage().get().not()) {
                                        activity?.toast("请开启“显示差分图”")
                                        return@GalleryCompose
                                    }
                                    presenter.mergeImages(it)
                                },
                                onEditUnionImage = {
                                    router.pushController(
                                        DiffGroupController(it.diffGroupId).withFadeInTransaction()
                                            .tag(DiffGroupController.TAG),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleEffect() {
        viewScope.launchUI {
            presenter.uiEffect.collectLatest { effect ->
                when (effect) {
                    is GalleryEffect.LaunchReader -> {
                        val id = effect.images.getOrElse(effect.index) {
                            effect.images.first()
                        }.id

                        ReaderActivity.newIntentToGallery(
                            activity!!,
                            galleryManager.putTempGallery(effect.images),
                            id,
                        ).apply {
                            startActivity(this)
                        }
                    }

                    is GalleryEffect.ShowToast -> {
                        activity?.toast(effect.message)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun sendEvent(eventBuild: () -> GalleryEvent) {
        presenter.sendEvent(eventBuild)
    }

    private fun getStatusBarHeight(activity: Activity): Int {
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        return insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
    }

    private fun getNavigationBarHeight(activity: Activity): Int {
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        return insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
    }
}
