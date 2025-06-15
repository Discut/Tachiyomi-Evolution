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
import androidx.compose.runtime.derivedStateOf
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
import eu.kanade.tachiyomi.ui.gallery.tags.TagsController
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.pxToDp
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.withFadeInTransaction
import uy.kohesive.injekt.injectLazy

class GalleryController(
    bundle: Bundle? = null,
    private var targetTag: TagBo? = null,
) :
    BaseCoroutineController<GalleryControllerBinding, GalleryPresenter>(bundle),
    RootSearchInterface {

    private val galleryManager: GalleryManager by injectLazy()

    override val presenter by lazy {
        GalleryPresenter(targetTag = targetTag)
    }

    override fun createBinding(inflater: LayoutInflater): GalleryControllerBinding {
        return GalleryControllerBinding.inflate(inflater)
    }

    override fun getTitle(): String? = null

    private fun getComposeTitle() = view?.context?.getString(R.string.gallery) ?: "Gallery"
    /*if (targetTag != null) {
        "\" ${targetTag?.tagValue} \""
    } else {
        view?.context?.getString(R.string.gallery)
    } ?: "Gallery"*/

    override fun getSearchTitle(): String {
        return "search"
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
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
                    val imageBOList = presenter.sourceImage.collectAsState(emptyList()).value
                    val listState by presenter.sourceImageSortDate.collectAsState()
                    val tags by presenter.tagsFlow.collectAsState(emptyList())
                    val selectedTags by presenter.selectedTags.collectAsState(emptyList())

                    val appbarTitle by remember(key1 = selectedTags) {
                        derivedStateOf {
                            if (selectedTags.isEmpty()) {
                                getComposeTitle()
                            } else {
                                "\" ${selectedTags.joinToString { it.tagValue }} \""
                            }
                        }
                    }
                    val preference: PreferencesHelper by injectLazy()

                    val isShowHideImage by preference.isShowHideImages().asFlow()
                        .collectAsState(true)

                    val resultImages by remember(key1 = isShowHideImage, key2 = listState) {
                        derivedStateOf {
                            if (isShowHideImage) {
                                listState
                            } else {
                                listState.map {
                                    it.key to it.value.filter { !it.isHide }
                                }.toMap()
                            }
                        }
                    }

                    GalleryCompose(
                        paddingValues = PaddingValues(
                            top = getStatusBarHeight(activity!!).pxToDp.dp,
                            bottom = getNavigationBarHeight(activity!!).pxToDp.dp,
                        ),

                        screenHeight = screenHeight!!,
                        scrollState = scrollState,
                        appbarTitle = appbarTitle,
                        tags = tags,
                        itemMap = resultImages,
                        isSelectedTags = selectedTags.isEmpty().not(),
                        onRandomPlay = outer@{
                            if (imageBOList.isEmpty()) {
                                activity?.toast("没有图片")
                                return@outer
                            }
                            presenter.randomImageList(imageBOList).apply {
                                // 图片点击处理
                                ReaderActivity.newIntentToGallery(
                                    activity!!,
                                    galleryManager.putTempGallery(this),
                                    first().id,
                                ).apply {
                                    startActivity(this)
                                }
                            }
                        },
                        onClearAllSelected = {
                            presenter.selectedTags.value = emptyList()
                        },
                        onClickJumpToTags = {
                            /*router.setRoot(
                                TagsController().withFadeInTransaction().tag(TagsController.ID),
                            )*/
                            router.pushController(
                                TagsController().withFadeInTransaction().tag(TagsController.ID),
                            )
                        },
                        onClickTag = presenter::clickTag,
                        onPlayGallery = { current, sources ->

                            val galleryId =
                                galleryManager.putTempGallery(sources.ifEmpty { imageBOList })
                            // 图片点击处理
                            ReaderActivity.newIntentToGallery(activity!!, galleryId, current.id)
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

    private fun getStatusBarHeight(activity: Activity): Int {
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        return insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
    }

    private fun getNavigationBarHeight(activity: Activity): Int {
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        return insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
    }
}
