package eu.kanade.tachiyomi.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.assist.AssistContent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.transition.addListener
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.gallery.GalleryExtensions.tryGetImage
import eu.kanade.tachiyomi.data.preference.asImmediateFlowIn
import eu.kanade.tachiyomi.data.preference.toggle
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.databinding.ReaderActivityBinding
import eu.kanade.tachiyomi.model.GalleryBo
import eu.kanade.tachiyomi.model.TagBo
import eu.kanade.tachiyomi.model.getTags
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.theme.GalleryTheme
import eu.kanade.tachiyomi.ui.base.MaterialMenuSheet
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.AddToLibraryFirst
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Success
import eu.kanade.tachiyomi.ui.reader.model.READER_MODE_KEY
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderMode
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.getReaderMode
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.settings.TabbedReaderSettingsSheet
import eu.kanade.tachiyomi.ui.reader.sheet.TagSettingsSheet
import eu.kanade.tachiyomi.ui.reader.slide.engine.ImageViewState
import eu.kanade.tachiyomi.ui.reader.slide.engine.KeyFrame
import eu.kanade.tachiyomi.ui.reader.slide.engine.SlideAnimationEngine
import eu.kanade.tachiyomi.ui.reader.slide.engine.SlideAnimationPath
import eu.kanade.tachiyomi.ui.reader.slide.engine.SlidePageHolder
import eu.kanade.tachiyomi.ui.reader.slide.engine.isEmpty
import eu.kanade.tachiyomi.ui.reader.slide.serializer.AnimationSequenceSaveManager
import eu.kanade.tachiyomi.ui.reader.slide.sheet.KeyframeEditSheet
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.KeyFrameTimelineView
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.AutoPlayTimer
import eu.kanade.tachiyomi.util.chapter.ChapterUtil.Companion.preferredChapterName
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.ThemeUtil
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.contextCompatDrawable
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getBottomGestureInsets
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.hasSideNavBar
import eu.kanade.tachiyomi.util.system.ignoredSystemInsets
import eu.kanade.tachiyomi.util.system.isBottomTappable
import eu.kanade.tachiyomi.util.system.isInNightMode
import eu.kanade.tachiyomi.util.system.isLTR
import eu.kanade.tachiyomi.util.system.isTablet
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.system.spToPx
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.system.withUIContext
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.compatToolTipText
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.hide
import eu.kanade.tachiyomi.util.view.isCollapsed
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.popupMenu
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.widget.doOnEnd
import eu.kanade.tachiyomi.widget.doOnStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Collections
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Activity containing the reader of Tachiyomi. This activity is mostly a container of the
 * viewers, to which calls from the view model or UI events are delegated.
 */
class ReaderActivity : BaseActivity<ReaderActivityBinding>() {

    /**
     * 阅读器阅读模式
     */
    var mode: ReaderMode = ReaderMode.MANGA

    val viewModel by viewModels<ReaderViewModel>()

    val scope = lifecycleScope

    /**
     * Viewer used to display the pages (pager, webtoon, ...).
     */
    var viewer: BaseViewer? = null
        private set

    /**
     * Whether the menu is currently visible.
     */
    var menuVisible = false
        private set

    /**
     * Whether the menu should stay visible.
     */
    private var menuTemporarilyVisible = false

    private var coroutine: Job? = null

    private var fromUrl = false

    /**
     * Configuration at reader level, like background color or forced orientation.
     */
    private var config: ReaderConfig? = null

    /**
     * Current Bottom Sheet on display, used to dismiss
     */
    private var bottomSheet: BottomSheetDialog? = null

    var sheetManageNavColor = false

    private val wic by lazy { WindowInsetsControllerCompat(window, binding.root) }
    private var lastVis = false

    private var snackbar: Snackbar? = null

    private var intentPageNumber: Int? = null

    var isLoading = false

    private var lastShiftDoubleState: Boolean? = null
    private var indexPageToShift: Int? = null
    private var indexChapterToShift: Long? = null

    private var lastCropRes = 0
    var manuallyShiftedPages = false
        private set

    val isSplitScreen: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode

    private var didTransitionFromChapter = false
    private var visibleChapterRange = longArrayOf()
    private var backPressedCallback: OnBackPressedCallback? = null

    var isScrollingThroughPagesOrChapters = false

    private var autoPlayTimer: AutoPlayTimer? = null
    private var hingeGapSize = 0
        set(value) {
            field = value
            (viewer as? PagerViewer)?.config?.hingeGapSize = value
        }

    // 时间轴同步：当前动画引擎引用
    private var currentAnimationEngine: SlideAnimationEngine? = null

    // 时间轴同步：上次更新时间（用于节流）
    private var lastTimelineUpdateTime = 0L
    private val TIMELINE_UPDATE_INTERVAL_MS = 33 // 每33ms更新一次（30fps）

    // 统一的时间轴数据管理
    private val currentAnimationPath: SlideAnimationPath?
        get() =
            currentAnimationEngine?.animationPath
    private val DEFAULT_DURATION = 5000L // 默认时长 5 秒

    // 当前页面的图片视图引用（用于获取变换状态）
    private var currentImageView: SubsamplingScaleImageView? = null
    private var currentPageHolder: SlidePageHolder? = null

    // 动画序列保存管理器
    private val animationSaveManager = AnimationSequenceSaveManager(this)

    private var loadAnimationJob: Job? = null

    // 关键帧编辑面板引用
    private var keyframeEditSheet: KeyframeEditSheet? = null

    // 当前选中的关键帧 index
    private var selectedKeyFrameIndex: Int? = null

    // 播放状态
    private var isPlaying = false

    companion object {
        private const val TAG = "ReaderActivity"

        const val SHIFT_DOUBLE_PAGES = "shiftingDoublePages"
        const val SHIFTED_PAGE_INDEX = "shiftedPageIndex"
        const val SHIFTED_CHAP_INDEX = "shiftedChapterIndex"
        const val GALLERY_ID = "gallery_id"
        const val TARGET_IMAGE_ID = "target_image_id"

        const val TRANSITION_NAME = "${BuildConfig.APPLICATION_ID}.TRANSITION_NAME"
        const val VISIBLE_CHAPTERS = "${BuildConfig.APPLICATION_ID}.VISIBLE_CHAPTERS"

        fun newIntent(context: Context, manga: Manga, chapter: Chapter): Intent {
            MainActivity.chapterIdToExitTo = 0L
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra("manga", manga.id)
            intent.putExtra("chapter", chapter.id)
            intent.putExtra(READER_MODE_KEY, ReaderMode.MANGA.key)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }

        fun newIntentToGallery(context: Context, galleryId: Long, targetImage: Long): Intent {
            MainActivity.chapterIdToExitTo = 0L
            val intent = Intent(context, ReaderActivity::class.java)

            intent.putExtra(READER_MODE_KEY, ReaderMode.GALLERY.key)
            intent.putExtra(GALLERY_ID, galleryId)
            intent.putExtra(TARGET_IMAGE_ID, targetImage)

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }

        fun newIntentWithTransitionOptions(activity: Activity, manga: Manga, chapter: Chapter, sharedElement: View): Pair<Intent, Bundle?> {
            MainActivity.chapterIdToExitTo = 0L
            val intent = newIntent(activity, manga, chapter)
            intent.putExtra(TRANSITION_NAME, sharedElement.transitionName)
            val activityOptions = ActivityOptions.makeSceneTransitionAnimation(
                activity,
                sharedElement,
                sharedElement.transitionName,
            )
            return intent to activityOptions.toBundle()
        }
    }

    /**
     * Called when the activity is created. Initializes the view model and configuration.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        mode = intent.extras.getReaderMode()
        // Setup shared element transitions
        if (intent.extras?.getString(TRANSITION_NAME) != null) {
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            findViewById<View>(android.R.id.content)?.let { contentView ->
                MainActivity.chapterIdToExitTo = 0L
                contentView.transitionName = intent.extras?.getString(TRANSITION_NAME)
                visibleChapterRange = intent.extras?.getLongArray(VISIBLE_CHAPTERS) ?: longArrayOf()
                didTransitionFromChapter = contentView.transitionName.contains("details chapter")
                setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
                window.sharedElementEnterTransition = buildContainerTransform(true)
                window.sharedElementReturnTransition = buildContainerTransform(false)
                // Postpone custom transition until manga ready
                postponeEnterTransition()
            }
        }

        super.onCreate(savedInstanceState)
        binding = ReaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val a = obtainStyledAttributes(intArrayOf(android.R.attr.windowLightStatusBar))
        val lightStatusBar = a.getBoolean(0, false)
        a.recycle()
        setCutoutMode()

        wic.isAppearanceLightStatusBars = lightStatusBar
        wic.isAppearanceLightNavigationBars = lightStatusBar

        binding.appBar.setBackgroundColor(contextCompatColor(R.color.surface_alpha))
        ViewCompat.setBackgroundTintList(
            binding.readerNav.root,
            ColorStateList.valueOf(contextCompatColor(R.color.surface_alpha)),
        )

        ViewCompat.setBackgroundTintList(
            binding.keyFrameTimeline.root,
            ColorStateList.valueOf(contextCompatColor(R.color.surface_alpha)),
        )

        backPressedCallback = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                if (binding.chaptersSheet.root.sheetBehavior.isExpanded()) {
                    binding.chaptersSheet.root.lastScale = binding.chaptersSheet.root.scaleX
                    binding.chaptersSheet.root.sheetBehavior?.collapse()
                }
                reEnableBackPressedCallBack()
            }

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                if (binding.chaptersSheet.root.sheetBehavior.isExpanded()) {
                    binding.chaptersSheet.root.sheetBehavior?.startBackProgress(backEvent)
                }
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (binding.chaptersSheet.root.sheetBehavior.isExpanded()) {
                    binding.chaptersSheet.root.sheetBehavior?.updateBackProgress(backEvent)
                }
            }

            override fun handleOnBackCancelled() {
                if (binding.chaptersSheet.root.sheetBehavior.isExpanded()) {
                    binding.chaptersSheet.root.sheetBehavior?.cancelBackProgress()
                }
            }
        }
        onBackPressedDispatcher.addCallback(backPressedCallback!!)
        if (viewModel.needsInit()) {
            fromUrl = handleIntentAction(intent)
            if (!fromUrl) {
                when (mode) {
                    ReaderMode.GALLERY -> {
                        val galleryId = intent.extras!!.getLong(GALLERY_ID, -1)
                        val targetId = intent.extras!!.getLong(TARGET_IMAGE_ID, -1)
                        if (galleryId == -1L || targetId == -1L) {
                            finish()
                            return
                        }

                        lifecycleScope.launchNonCancellable {
                            val initResult = viewModel.initGallery(galleryId, targetId)
                            if (!initResult.getOrDefault(false)) {
                                val exception = initResult.exceptionOrNull()
                                    ?: IllegalStateException("Unknown err")
                                withUIContext {
                                    setInitialChapterError(exception)
                                }
                            }
                        }
                    }

                    ReaderMode.MANGA -> {
                        val manga = intent.extras!!.getLong("manga", -1)
                        val chapter = intent.extras!!.getLong("chapter", -1)
                        if (manga == -1L || chapter == -1L) {
                            finish()
                            return
                        }
                        lifecycleScope.launchNonCancellable {
                            val initResult = viewModel.init(manga, chapter)
                            if (!initResult.getOrDefault(false)) {
                                val exception = initResult.exceptionOrNull()
                                    ?: IllegalStateException("Unknown err")
                                withUIContext {
                                    setInitialChapterError(exception)
                                }
                            }
                        }
                    }
                }
            } else {
                binding.pleaseWait.isVisible = true
            }
        }

        if (savedInstanceState != null) {
            menuVisible = savedInstanceState.getBoolean(::menuVisible.name)
            lastShiftDoubleState = savedInstanceState.getBoolean(SHIFT_DOUBLE_PAGES)
                .takeIf { savedInstanceState.containsKey(SHIFT_DOUBLE_PAGES) }
            indexPageToShift = savedInstanceState.getInt(SHIFTED_PAGE_INDEX, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }
            indexChapterToShift = savedInstanceState.getLong(SHIFTED_CHAP_INDEX, Long.MIN_VALUE)
                .takeIf { it != Long.MIN_VALUE }
            binding.readerNav.root.isInvisible = !menuVisible
            binding.keyFrameTimeline.root.isInvisible = !menuVisible || mode != ReaderMode.GALLERY
        } else {
            binding.readerNav.root.isInvisible = true
            binding.keyFrameTimeline.root.isInvisible = true
        }

        binding.chaptersSheet.chaptersBottomSheet.setup(this)
        config = ReaderConfig()
        initializeMenu()

        preferences.incognitoMode()
            .asImmediateFlowIn(lifecycleScope) {
                SecureActivityDelegate.setSecure(this)
            }
        reEnableBackPressedCallBack()

        viewModel.state
            .map { it.isLoadingAdjacentChapter }
            .distinctUntilChanged()
            .onEach(::setProgressDialog)
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.manga }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::setManga)
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.viewerChapters }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::setChapters)
            .launchIn(lifecycleScope)

        viewModel.eventFlow
            .onEach { event ->
                when (event) {
                    ReaderViewModel.Event.ReloadMangaAndChapters -> {
                        viewModel.manga?.let(::setManga)
                        viewModel.state.value.viewerChapters?.let(::setChapters)
                    }
                    ReaderViewModel.Event.ReloadViewerChapters -> {
                        viewModel.state.value.viewerChapters?.let(::setChapters)
                    }
                    is ReaderViewModel.Event.SetOrientation -> {
                        setOrientation(event.orientation)
                    }
                    is ReaderViewModel.Event.SavedImage -> {
                        onSaveImageResult(event.result)
                    }
                    is ReaderViewModel.Event.ShareImage -> {
                        onShareImageResult(event.file, event.page)
                    }
                    is ReaderViewModel.Event.SetCoverResult -> {
                        onSetAsCoverResult(event.result)
                    }
                    is ReaderViewModel.Event.ShareTrackingError -> {
                        showTrackingError(event.errors)
                    }
                }
            }
            .launchIn(lifecycleScope)

        lifecycleScope.launchUI {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@ReaderActivity).windowLayoutInfo(this@ReaderActivity)
                    .collect { newLayoutInfo ->
                        hingeGapSize = 0
                        for (displayFeature: DisplayFeature in newLayoutInfo.displayFeatures) {
                            if (displayFeature is FoldingFeature && displayFeature.occlusionType == FoldingFeature.OcclusionType.FULL &&
                                displayFeature.isSeparating && displayFeature.orientation == FoldingFeature.Orientation.VERTICAL
                            ) {
                                hingeGapSize = displayFeature.bounds.width()
                            }
                        }
                        if (hingeGapSize > 0) {
                            binding.navLayout.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                                gravity = Gravity.TOP or Gravity.CENTER
                                anchorGravity = Gravity.TOP or Gravity.CENTER
                                width = (binding.root.width - hingeGapSize) / 2 - 24.dpToPx
                            }
                            binding.chaptersSheet.root.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                                gravity = Gravity.END
                                width = (binding.root.width - hingeGapSize) / 2
                            }
                            binding.pleaseWait.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                                marginStart = binding.root.width / 2 + hingeGapSize
                            }
                        }
                    }
            }
        }

        binding.tagsEditAndShowContainer.setContent {
            GalleryTheme {
                val isEnable by preferences.quickLabeling().asFlow().collectAsState(false)

                if (isEnable.not()) {
                    return@GalleryTheme
                }

                val currentPage by viewer?.currentPageAsFlow()?.collectAsState(null) ?: return@GalleryTheme
                var tags by remember { mutableStateOf(emptyList<TagBo>()) }
                var isLoaded by remember { mutableStateOf(false) }
                LaunchedEffect(currentPage) {
                    isLoaded = false
                    tags = currentPage.tryGetImage()?.getTags() ?: emptyList()
                    if (tags.isEmpty()) {
                        isLoaded = false
                        return@LaunchedEffect
                    }
                    delay(500)
                    isLoaded = true
                }
                TagsAndEdit(
                    tags = tags,
                    isLoaded = isLoaded,
                    onClickEdit = {
                        currentPage?.apply {
                            showMarkDialog(this)
                        }
                    },
                    onClickTag = { tagBo ->
                    },
                )
            }
        }

        // 播放/暂停按钮
        binding.keyFrameTimeline.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        // 上一个关键帧
        binding.keyFrameTimeline.btnPrevKeyframe.setOnClickListener {
            navigateToPreviousKeyFrame()
        }

        // 下一个关键帧
        binding.keyFrameTimeline.btnNextKeyframe.setOnClickListener {
            navigateToNextKeyFrame()
        }

        // 添加关键帧
        binding.keyFrameTimeline.btnAddKeyframe.setOnClickListener {
            addKeyFrameAtCurrentTime()
        }

        // 编辑面板按钮
        binding.keyFrameTimeline.btnExpandEdit.setOnClickListener {
            openKeyframeEditSheet()
        }
    }

    /**
     * 播放/暂停切换
     */
    private fun togglePlayPause() {
        val engine = currentAnimationEngine ?: run {
            toast("动画引擎未初始化")
            return
        }

        if (isPlaying) {
            // 暂停
            engine.pause()
            isPlaying = false
            binding.keyFrameTimeline.btnPlayPause.setImageResource(R.drawable.ic_play_arrow_24dp)
        } else {
            // 播放
            val path = currentAnimationPath
            if (path == null || path.keyFrames.isEmpty()) {
                toast("没有关键帧，无法播放")
                return
            }

            // 首次播放时放宽平移限制
            engine.relaxPanLimit()

            if (engine.elapsedTime >= path.durationMs) {
                // 如果已到结尾，重置后从头播放
                engine.stop()
                engine.elapsedTime = 0L
            }

            // 如果引擎处于暂停状态（isRunning=true，但帧回调已移除），调用 resume
            if (engine.isActive()) {
                engine.resume()
            } else {
                engine.start()
            }

            isPlaying = true
            binding.keyFrameTimeline.btnPlayPause.setImageResource(R.drawable.ic_pause_24dp)
        }
    }

    /**
     * 导航到上一个关键帧
     */
    private fun navigateToPreviousKeyFrame() {
        val path = currentAnimationPath ?: return
        val engine = currentAnimationEngine ?: return
        if (path.keyFrames.isEmpty()) return

        val keyFrames = path.keyFrames.sortedBy { it.timeMs }
        val currentTime = engine.elapsedTime

        val prevIndex = keyFrames.indexOfLast { it.timeMs < currentTime - 100 }
            .takeIf { it >= 0 } ?: 0
        val prevTime = keyFrames[prevIndex].timeMs

        engine.elapsedTime = prevTime
        engine.reCalculateStateAndApply()
        binding.keyFrameTimeline.keyFrameTimelineBody.seekToTime(prevTime)
        binding.keyFrameTimeline.keyFrameTimelineBody.selectKeyFrame(prevTime)
        selectedKeyFrameIndex = prevIndex
        updateTimeDisplay()
        updateKeyframeEditSheetIfOpen(prevIndex)
    }

    /**
     * 导航到下一个关键帧
     */
    private fun navigateToNextKeyFrame() {
        val path = currentAnimationPath ?: return
        val engine = currentAnimationEngine ?: return
        if (path.keyFrames.isEmpty()) return

        val keyFrames = path.keyFrames.sortedBy { it.timeMs }
        val currentTime = engine.elapsedTime

        val nextIndex = keyFrames.indexOfFirst { it.timeMs > currentTime + 100 }
            .takeIf { it >= 0 } ?: (keyFrames.size - 1)
        val nextTime = keyFrames[nextIndex].timeMs

        engine.elapsedTime = nextTime
        engine.reCalculateStateAndApply()
        binding.keyFrameTimeline.keyFrameTimelineBody.seekToTime(nextTime)
        binding.keyFrameTimeline.keyFrameTimelineBody.selectKeyFrame(nextTime)
        selectedKeyFrameIndex = nextIndex
        updateTimeDisplay()
        updateKeyframeEditSheetIfOpen(nextIndex)
    }

    /**
     * 打开关键帧编辑面板 (BottomSheet)
     */
    private fun openKeyframeEditSheet() {
        val path = currentAnimationPath ?: run {
            toast("时间轴未初始化")
            return
        }
        val isEmpty = path.keyFrames.isEmpty()
        val index = selectedKeyFrameIndex ?: 0
        val keyFrame = if (isEmpty) {
            KeyFrame.default()
        } else {
            path.keyFrames.getOrNull(index) ?: path.keyFrames.first()
        }

        android.util.Log.d(TAG, "openKeyframeEditSheet: index=$index, timeMs=${keyFrame.timeMs}")

        keyframeEditSheet = KeyframeEditSheet(
            activity = this,
            initialKeyFrameTimeMs = keyFrame.timeMs,
            initialImageViewState = keyFrame.state,
            animationPath = path,
            isEmptyKeyFrames = isEmpty,
            onPropertyChanged = { timeMs, newState ->
                updateKeyFrameState(timeMs, newState)
            },
            onDeleteRequested = { timeMs ->
                deleteKeyFrameWithUndo(timeMs)
            },
            onDuplicateRequested = { timeMs ->
                duplicateKeyFrame(timeMs)
            },
            onDurationChanged = { newDurationMs ->
                updateAnimationDuration(newDurationMs)
            },
            onCatchRequested = { timeMs ->
                catchStateOfImageToKeyFrame(timeMs)
            },
        ).apply {
            show()
        }
    }

    /**
     * 更新关键帧状态（实时预览）
     */
    private fun updateKeyFrameState(timeMs: Long, newState: ImageViewState) {
        val path = currentAnimationPath ?: return
        val newKeyFrames = path.keyFrames.map {
            if (it.timeMs == timeMs) eu.kanade.tachiyomi.ui.reader.slide.engine.KeyFrame(timeMs, newState) else it
        }

        val newPath = path.copy(keyFrames = newKeyFrames)
        currentAnimationEngine?.let {
            it.animationPath = newPath
            it.applyState(newState)
        }
        currentPageHolder?.animationPath = newPath
        syncAnimationToSaveManager()
    }

    /**
     * 删除关键帧（带撤销）
     */
    private fun deleteKeyFrameWithUndo(timeMs: Long) {
        val path = currentAnimationPath ?: return
        val originalKeyFrames = path.keyFrames.toList()
        val index = originalKeyFrames.indexOfFirst { it.timeMs == timeMs }
        if (index == -1) {
            toast("关键帧不存在")
            return
        }

        val deletedFrame = originalKeyFrames[index]
        val newKeyFrames = originalKeyFrames.toMutableList().apply { removeAt(index) }

        val newPath = path.copy(keyFrames = newKeyFrames)
        currentAnimationEngine?.animationPath = newPath
        currentPageHolder?.animationPath = newPath
        syncAnimationToSaveManager()
        binding.keyFrameTimeline.keyFrameTimelineBody.invalidate()
        binding.keyFrameTimeline.keyFrameTimelineBody.selectKeyFrame(null)
        selectedKeyFrameIndex = null
        updateTimeDisplay()

        android.util.Log.d(TAG, "deleted keyframe[$index] at ${deletedFrame.timeMs}ms")

        // 显示撤销 Snackbar
        Snackbar.make(binding.root, "已删除关键帧: ${deletedFrame.timeMs / 1000f}s", Snackbar.LENGTH_LONG)
            .setAction("撤销") {
                val restoredPath = path.copy(keyFrames = originalKeyFrames)
                currentAnimationEngine?.animationPath = restoredPath
                currentPageHolder?.animationPath = restoredPath
                syncAnimationToSaveManager()
                binding.keyFrameTimeline.keyFrameTimelineBody.invalidate()
                binding.keyFrameTimeline.keyFrameTimelineBody.selectKeyFrame(deletedFrame.timeMs)
                selectedKeyFrameIndex = index
                updateTimeDisplay()
            }
            .show()
    }

    /**
     * 捕获当前图片状态作为关键帧
     */
    private fun catchStateOfImageToKeyFrame(timeMs: Long) {
        val imageView = currentImageView
        if (imageView == null || !imageView.isReady) {
            toast("图片未准备好")
            return
        }

        currentAnimationPath ?: run {
            toast("时间轴未初始化")
            return
        }

        updateKeyFrameState(timeMs, getImageViewState(imageView))
        toast("已捕获当前图片状态")
    }

    /**
     * 复制关键帧
     */
    private fun duplicateKeyFrame(timeMs: Long) {
        val path = currentAnimationPath ?: return
        val keyFrame = path.keyFrames.firstOrNull { it.timeMs == timeMs } ?: return

        val newTimeMs = (timeMs + 500).coerceAtMost(path.durationMs)

        // 修复：原逻辑反了，find 返回 null 时反而报"已存在"
        path.keyFrames.find { it.timeMs == newTimeMs }?.let {
            toast("已存在相同时间点的关键帧")
            return
        }

        val newKeyFrame = eu.kanade.tachiyomi.ui.reader.slide.engine.KeyFrame(newTimeMs, keyFrame.state)
        val newKeyFrames = (path.keyFrames + newKeyFrame).sortedBy { it.timeMs }

        val newPath = path.copy(keyFrames = newKeyFrames)
        currentAnimationEngine?.animationPath = newPath
        currentPageHolder?.animationPath = newPath
        syncAnimationToSaveManager()
        binding.keyFrameTimeline.keyFrameTimelineBody.invalidate()

        android.util.Log.d(TAG, "duplicated keyframe from ${timeMs}ms to ${newTimeMs}ms")
        toast("已复制关键帧到: ${newTimeMs / 1000f}s")
    }

    /**
     * 更新动画总时长
     */
    private fun updateAnimationDuration(newDurationMs: Long) {
        val path = currentAnimationPath ?: return
        val lastKeyFrameTime = path.keyFrames.maxOfOrNull { it.timeMs } ?: 0L
        val clamped = newDurationMs.coerceIn(lastKeyFrameTime.coerceAtLeast(1000L), 60000L)

        val newPath = path.copy(durationMs = clamped)
        currentAnimationEngine?.animationPath = newPath
        currentPageHolder?.animationPath = newPath
        syncAnimationToSaveManager()
        binding.keyFrameTimeline.keyFrameTimelineBody.invalidate()
        updateTimeDisplay()
    }

    /**
     * 同步动画数据到保存管理器
     */
    private fun syncAnimationToSaveManager() {
        currentAnimationPath?.let {
            animationSaveManager.setCurrentAnimation(animationSaveManager.getCurrentImageId(), it)
        }
        animationSaveManager.markAsDirty()
    }

    /**
     * 更新时间显示
     */
    private fun updateTimeDisplay() {
        val currentTime = currentAnimationEngine?.elapsedTime ?: 0L
        val duration = currentAnimationPath?.durationMs ?: DEFAULT_DURATION
        binding.keyFrameTimeline.timelineTimeDisplay.text = "${(currentTime.toFloat() / 1000f).format(1)}/${(duration.toFloat() / 1000f).format(1)}s"
    }

    private fun Float.format(digits: Int): String = "%.${digits}f".format(this)

    /**
     * 如果编辑面板已打开，更新其显示的关键帧
     */
    private fun updateKeyframeEditSheetIfOpen(index: Int) {
        val sheet = keyframeEditSheet ?: return
        if (!sheet.isShowing) {
            keyframeEditSheet = null
            return
        }
        val path = currentAnimationPath ?: return
        val keyFrame = path.keyFrames.getOrNull(index) ?: return
        sheet.updateKeyFrameTime(keyFrame.timeMs, keyFrame.state)
    }

    /**
     * 在当前时间轴光标位置添加关键帧
     */
    private fun addKeyFrameAtCurrentTime() {
        val imageView = currentImageView
        if (imageView == null || !imageView.isReady) {
            toast("图片未准备好")
            return
        }

        val path = currentAnimationPath ?: run {
            toast("时间轴未初始化")
            return
        }

        // 获取当前时间轴光标位置对应的时间
        val currentTime = binding.keyFrameTimeline.keyFrameTimelineBody.getCurrentTime()

        // 检查是否已有关键帧在相同时间位置
        val existingFrameIndex = path.keyFrames.indexOfFirst { it.timeMs == currentTime }
        if (existingFrameIndex != -1) {
            toast("该时间位置已有关键帧，请先删除或调整光标位置")
            return
        }

        // 从 ImageView 获取当前变换状态
        val state = getImageViewState(imageView)

        // 创建新的关键帧
        val newKeyFrame = eu.kanade.tachiyomi.ui.reader.slide.engine.KeyFrame(
            timeMs = currentTime,
            state = state,
        )

        // 添加到页面动画路径，并按时间排序
        val newKeyFrames = path.keyFrames.toMutableList()
        newKeyFrames.add(newKeyFrame)
        newKeyFrames.sortBy { it.timeMs }

        // 更新当前动画路径
        currentAnimationEngine?.animationPath = path.copy(keyFrames = newKeyFrames)

        // 更新页面的 animationPath 引用（这样页面切换时数据会保留）
        currentAnimationPath?.apply {
            currentPageHolder?.animationPath = this
        }

        // 刷新时间轴显示
        binding.keyFrameTimeline.keyFrameTimelineBody.invalidate()

        // 同步到保存管理器
        syncAnimationToSaveManager()

        // 刷新图片视图，显示当前时间点的插值状态
        currentAnimationEngine?.reCalculateStateAndApply()

        toast("已添加关键帧: ${currentTime / 1000f}s")
    }

    /**
     * 从 SubsamplingScaleImageView 获取当前的 ImageViewState（使用归一化值 V3）
     *
     * 归一化说明：
     * - normalizedSourceCenterX/Y: 相对于图片原始尺寸（sWidth/sHeight）的比例，0.5=图片中心
     * - normalizedScale: 相对于 fitScale 的倍数
     *
     * 新坐标系直接存储 sourceCenter，避免 translation → sourceCenter 转换在横竖屏切换时的误差。
     * sWidth/sHeight 是图片原始像素尺寸，在不同屏幕方向上保持不变。
     */
    private fun getImageViewState(imageView: SubsamplingScaleImageView): eu.kanade.tachiyomi.ui.reader.slide.engine.ImageViewState {
        val currentCenter = imageView.center
        val sourceCenterX = currentCenter?.x ?: (imageView.sWidth / 2f)
        val sourceCenterY = currentCenter?.y ?: (imageView.sHeight / 2f)

        // 获取当前缩放和适配屏幕的缩放值
        val currentScale = imageView.scale
        val fitScale = imageView.minScale

        // 获取图片原始尺寸（sWidth/sHeight 是图片原始像素尺寸，不同屏幕方向上保持不变）
        val imageWidth = imageView.sWidth
        val imageHeight = imageView.sHeight

        return eu.kanade.tachiyomi.ui.reader.slide.engine.createNormalizedImageViewState(
            sourceCenterX = sourceCenterX,
            sourceCenterY = sourceCenterY,
            currentScale = currentScale,
            fitScale = fitScale,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            rotation = imageView.rotation,
            alpha = imageView.alpha,
        )
    }

    /**
     * 保存关键帧序列
     */
    private fun saveTimePoint(onEnd: suspend () -> Unit = {}) {
        if (animationSaveManager.hasUnsavedChanges()) {
            scope.launchIO {
                animationSaveManager.saveImmediately()
                onEnd()
            }
        }
    }

    /**
     * Called when the activity is destroyed. Cleans up the viewer, configuration and any view.
     */
    override fun onDestroy() {
        // ============================================
        // 退出阅读器时保存待保存的动画序列并释放资源
        // ============================================
        if (animationSaveManager.hasUnsavedChanges()) {
            saveTimePoint {
                animationSaveManager.dispose()
            }
        } else {
            animationSaveManager.dispose()
        }

        super.onDestroy()
        viewer?.destroy()
        binding.chaptersSheet.chaptersBottomSheet.adapter = null
        viewer = null
        config = null
        bottomSheet?.dismiss()
        bottomSheet = null
        snackbar?.dismiss()
        snackbar = null
        autoPlayTimer?.cancelTickAndProgress()
        // 清理动画引擎回调，避免内存泄漏
        currentAnimationEngine?.restorePanLimit()
        currentAnimationEngine?.onFrameUpdate = null
        currentAnimationEngine = null
        currentImageView = null
    }

    /**
     * Called when the activity is saving instance state. Current progress is persisted if this
     * activity isn't changing configurations.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(::menuVisible.name, menuVisible)
        (viewer as? PagerViewer)?.let { pViewer ->
            val config = pViewer.config
            if (config.doublePages) {
                outState.putBoolean(SHIFT_DOUBLE_PAGES, config.shiftDoublePage)
            }
            if (config.shiftDoublePage && config.doublePages) {
                pViewer.getShiftedPage()?.let {
                    outState.putInt(SHIFTED_PAGE_INDEX, it.index)
                    outState.putLong(SHIFTED_CHAP_INDEX, it.chapter.chapterId)
                }
            }
        }
        viewModel.onSaveInstanceState()
        super.onSaveInstanceState(outState)
    }

    /**
     * Called when the options menu of the binding.toolbar is being created. It adds our custom menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reader, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val splitItem = menu.findItem(R.id.action_shift_double_page)
        splitItem?.isVisible = ((viewer as? PagerViewer)?.config?.doublePages ?: false) && !canShowSplitAtBottom()
        binding.chaptersSheet.shiftPageButton.isVisible = ((viewer as? PagerViewer)?.config?.doublePages ?: false) && canShowSplitAtBottom()
        (viewer as? PagerViewer)?.config?.let { config ->
            val icon = ContextCompat.getDrawable(
                this,
                if ((!config.shiftDoublePage).xor(viewer is R2LPagerViewer)) R.drawable.ic_page_previous_outline_24dp else R.drawable.ic_page_next_outline_24dp,
            )
            splitItem?.icon = icon
            binding.chaptersSheet.shiftPageButton.setImageDrawable(icon)
        }
        setBottomNavButtons(preferences.pageLayout().get())
        (binding.toolbar.background as? LayerDrawable)?.let { layerDrawable ->
            val isDoublePage = splitItem?.isVisible ?: false
            // Shout out to Google for not fixing setVisible https://issuetracker.google.com/issues/127538945
            layerDrawable.findDrawableByLayerId(R.id.layer_full_width).alpha = if (!isDoublePage) 255 else 0
            layerDrawable.findDrawableByLayerId(R.id.layer_one_item).alpha = if (isDoublePage) 255 else 0
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun canShowSplitAtBottom(): Boolean {
        return if (preferences.readerBottomButtons().isNotSet()) {
            isTablet()
        } else {
            ReaderBottomButton.ShiftDoublePage.isIn(preferences.readerBottomButtons().get())
        }
    }

    fun setBottomNavButtons(pageLayout: Int) {
        val isDoublePage = pageLayout == PageLayout.DOUBLE_PAGES.value ||
            (pageLayout == PageLayout.AUTOMATIC.value && (viewer as? PagerViewer)?.config?.doublePages ?: false)
        binding.chaptersSheet.doublePage.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                when {
                    isDoublePage -> R.drawable.ic_book_open_variant_24dp
                    (viewer as? PagerViewer)?.config?.splitPages == true -> R.drawable.ic_book_open_split_24dp
                    else -> R.drawable.ic_single_page_24dp
                },
            ),
        )
        with(binding.readerNav) {
            listOf(leftPageText, rightPageText).forEach {
                it.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    val isCurrent = (viewer is R2LPagerViewer).xor(it === leftPageText)
                    width = if (isDoublePage && isCurrent) 48.spToPx else 32.spToPx
                }
            }
        }
    }

    private fun updateOrientationShortcut(preference: Int) {
        val orientation = OrientationType.fromPreference(preference)
        binding.chaptersSheet.rotationSheetButton.setImageResource(orientation.iconRes)
    }

    private fun updateCropBordersShortcut() {
        val isPagerType = viewer is PagerViewer || (viewer as? WebtoonViewer)?.hasMargins == true
        val enabled = if (isPagerType) {
            preferences.cropBorders().get()
        } else {
            preferences.cropBordersWebtoon().get()
        }

        with(binding.chaptersSheet.cropBordersSheetButton) {
            val drawableRes = if (enabled) {
                R.drawable.anim_free_to_crop
            } else {
                R.drawable.anim_crop_to_free
            }
            if (lastCropRes != drawableRes) {
                val drawable = AnimatedVectorDrawableCompat.create(context, drawableRes)
                setImageDrawable(drawable)
                drawable?.start()
                lastCropRes = drawableRes
            }
            compatToolTipText =
                getString(
                    if (enabled) {
                        R.string.remove_crop
                    } else {
                        R.string.crop_borders
                    },
                )
        }
    }

    private fun updateBottomShortcuts() {
        val enabledButtons = preferences.readerBottomButtons().get()
        with(binding.chaptersSheet) {
            readingMode.isVisible = ReaderBottomButton.ReadingMode.isIn(enabledButtons)
            rotationSheetButton.isVisible =
                ReaderBottomButton.Rotation.isIn(enabledButtons)
            doublePage.isVisible = viewer is PagerViewer &&
                ReaderBottomButton.PageLayout.isIn(enabledButtons)
            cropBordersSheetButton.isVisible =
                if (viewer is PagerViewer) {
                    ReaderBottomButton.CropBordersPaged.isIn(enabledButtons)
                } else {
                    ReaderBottomButton.CropBordersWebtoon.isIn(enabledButtons)
                }
            webviewButton.isVisible =
                ReaderBottomButton.WebView.isIn(enabledButtons) && mode != ReaderMode.GALLERY
            chaptersButton.isVisible =
                ReaderBottomButton.ViewChapters.isIn(enabledButtons) && mode != ReaderMode.GALLERY
            shiftPageButton.isVisible =
                ((viewer as? PagerViewer)?.config?.doublePages ?: false) && canShowSplitAtBottom()
            binding.toolbar.menu.findItem(R.id.action_shift_double_page)?.isVisible =
                ((viewer as? PagerViewer)?.config?.doublePages ?: false) && !canShowSplitAtBottom()
        }
    }

    /**
     * Called when an item of the options menu was clicked. Used to handle clicks on our menu
     * entries.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_shift_double_page -> {
                shiftDoublePages()
                manuallyShiftedPages = true
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun shiftDoublePages(forceShift: Boolean? = null, page: ReaderPage? = null) {
        (viewer as? PagerViewer)?.let { pViewer ->
            if (forceShift == pViewer.config.shiftDoublePage) return
            pViewer.config.shiftDoublePage = !pViewer.config.shiftDoublePage
            viewModel.state.value.viewerChapters?.let {
                pViewer.updateShifting(page)
                pViewer.setChaptersDoubleShift(it)
                invalidateOptionsMenu()
            }
        }
    }

    private fun popToMain() {
        if (fromUrl) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finishAfterTransition()
        } else {
            backPressedCallback?.isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun reEnableBackPressedCallBack() {
        backPressedCallback?.isEnabled = binding.chaptersSheet.chaptersBottomSheet.sheetBehavior.isExpanded()
    }

    override fun finishAfterTransition() {
        if (didTransitionFromChapter && visibleChapterRange.isNotEmpty() && MainActivity.chapterIdToExitTo !in visibleChapterRange) {
            finish()
        } else {
            viewModel.onBackPressed()
            super.finishAfterTransition()
        }
    }

    override fun finish() {
        viewModel.onBackPressed()
        super.finish()
    }

    /**
     * Dispatches a key event. If the viewer doesn't handle it, call the default implementation.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = viewer?.handleKeyEvent(event) ?: false
        return handled || super.dispatchKeyEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_N -> {
                if (viewer is R2LPagerViewer) {
                    binding.readerNav.leftChapter.performClick()
                } else {
                    binding.readerNav.rightChapter.performClick()
                }
                return true
            }
            KeyEvent.KEYCODE_P -> {
                if (viewer !is R2LPagerViewer) {
                    binding.readerNav.leftChapter.performClick()
                } else {
                    binding.readerNav.rightChapter.performClick()
                }
                return true
            }
            KeyEvent.KEYCODE_L -> {
                binding.readerNav.leftChapter.performClick()
                return true
            }
            KeyEvent.KEYCODE_R -> {
                binding.readerNav.rightChapter.performClick()
                return true
            }
            KeyEvent.KEYCODE_E -> {
                viewer?.moveToNext()
                return true
            }
            KeyEvent.KEYCODE_Q -> {
                viewer?.moveToPrevious()
                return true
            }
            else -> return super.onKeyUp(keyCode, event)
        }
    }

    /**
     * Dispatches a generic motion event. If the viewer doesn't handle it, call the default
     * implementation.
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = viewer?.handleGenericMotionEvent(event) ?: false
        return handled || super.dispatchGenericMotionEvent(event)
    }

    private fun buildContainerTransform(entering: Boolean): MaterialContainerTransform {
        return MaterialContainerTransform(this, entering).apply {
            duration = (
                resources?.getInteger(
                    if (entering) {
                        android.R.integer.config_longAnimTime
                    } else {
                        android.R.integer.config_mediumAnimTime
                    },
                ) ?: 500
                ).toLong()
            addTarget(android.R.id.content)
        }
    }

    /**
     * Initializes the reader menu. It sets up click listeners and the initial visibility.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeMenu() {
        // Set binding.toolbar
        setSupportActionBar(binding.toolbar)
        val primaryColor = ColorUtils.setAlphaComponent(
            getResourceColor(R.attr.colorSurface),
            200,
        )
        binding.appBar.setBackgroundColor(primaryColor)
        window.statusBarColor = Color.TRANSPARENT
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.navigationIcon?.setTint(getResourceColor(R.attr.actionBarTintColor))
        binding.toolbar.setNavigationOnClickListener {
            popToMain()
        }

        binding.toolbar.setOnClickListener {
            if (mode == ReaderMode.GALLERY) {
                Timber.i("Gallery 不跳转到详情页")
                return@setOnClickListener
            }
            viewModel.manga?.id?.let { id ->
                val intent = SearchActivity.openMangaIntent(this, id)
                startActivity(intent)
            }
        }

        with(binding.chaptersSheet) {
            with(doublePage) {
                compatToolTipText = getString(R.string.page_layout)
                setOnClickListener {
                    if (preferences.pageLayout().get() == PageLayout.AUTOMATIC.value) {
                        (viewer as? PagerViewer)?.config?.let { config ->
                            config.doublePages = !config.doublePages
                            reloadChapters(config.doublePages, true)
                        }
                    } else {
                        showPageLayoutMenu()
                    }
                }
                setOnLongClickListener {
                    showPageLayoutMenu()
                    true
                }
            }
            cropBordersSheetButton.setOnClickListener {
                val pref =
                    if ((viewer as? WebtoonViewer)?.hasMargins == true ||
                        (viewer is PagerViewer)
                    ) {
                        preferences.cropBorders()
                    } else {
                        preferences.cropBordersWebtoon()
                    }
                pref.toggle()
            }

            with(rotationSheetButton) {
                compatToolTipText = getString(R.string.rotation)

                setOnClickListener {
                    popupMenu(
                        items = OrientationType.entries.map { it.flagValue to it.stringRes },
                        selectedItemId = viewModel.manga?.orientationType
                            ?: preferences.defaultOrientationType().get(),
                    ) {
                        val newOrientation = OrientationType.fromPreference(itemId)

                        viewModel.setMangaOrientationType(newOrientation.flagValue)

                        updateOrientationShortcut(newOrientation.flagValue)
                    }
                }
            }

            webviewButton.setOnClickListener {
                openMangaInBrowser()
            }

            displayOptions.setOnClickListener {
                TabbedReaderSettingsSheet(this@ReaderActivity).show()
            }

            displayOptions.setOnLongClickListener {
                TabbedReaderSettingsSheet(this@ReaderActivity, true).show()
                true
            }

            readingMode.setOnClickListener { readingMode ->
                readingMode.popupMenu(
                    items = ReadingModeType.entries.map { it.flagValue to it.stringRes },
                    selectedItemId = viewModel.manga?.readingModeType,
                ) {
                    viewModel.setMangaReadingMode(itemId)
                }
            }
        }

        listOf(preferences.cropBorders(), preferences.cropBordersWebtoon())
            .forEach { pref ->
                pref.asFlow()
                    .onEach { updateCropBordersShortcut() }
                    .launchIn(scope)
            }

        binding.chaptersSheet.shiftPageButton.setOnClickListener {
            shiftDoublePages()
            manuallyShiftedPages = true
        }

        binding.readerNav.leftChapter.setOnClickListener { loadAdjacentChapter(false) }
        binding.readerNav.rightChapter.setOnClickListener { loadAdjacentChapter(true) }

        binding.touchView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (binding.chaptersSheet.chaptersBottomSheet.sheetBehavior.isExpanded()) {
                    binding.chaptersSheet.chaptersBottomSheet.sheetBehavior?.collapse()
                }
            }
            false
        }
        val readerNavGestureDetector = ReaderNavGestureDetector(this)
        val gestureDetector = GestureDetectorCompat(this, readerNavGestureDetector)
        with(binding.readerNav) {
            binding.readerNav.pageSeekbar.addOnSliderTouchListener(
                object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {
                        readerNavGestureDetector.lockVertical = false
                        readerNavGestureDetector.hasScrollHorizontal = true
                        isScrollingThroughPagesOrChapters = true
                    }

                    override fun onStopTrackingTouch(slider: Slider) {
                        isScrollingThroughPagesOrChapters = false
                    }
                },
            )
            listOf(root, leftChapter, rightChapter, pageSeekbar).forEach {
                it.setOnTouchListener { _, event ->
                    val result = gestureDetector.onTouchEvent(event)
                    if (event?.action == MotionEvent.ACTION_UP) {
                        if (!result) {
                            val sheetBehavior = binding.chaptersSheet.root.sheetBehavior
                            if (sheetBehavior?.state != BottomSheetBehavior.STATE_SETTLING && !sheetBehavior.isCollapsed()) {
                                sheetBehavior?.collapse()
                            }
                        }
                        if (readerNavGestureDetector.lockVertical) {
                            return@setOnTouchListener true
                        }
                    } else if ((event?.action != MotionEvent.ACTION_UP || event.action != MotionEvent.ACTION_DOWN) && result) {
                        event.action = MotionEvent.ACTION_CANCEL
                        return@setOnTouchListener false
                    }
                    if (it == pageSeekbar) {
                        readerNavGestureDetector.lockVertical
                    } else {
                        result
                    }
                }
            }
        }

        // Init listeners on bottom menu
        binding.readerNav.pageSeekbar.addOnChangeListener { _, value, fromUser ->
            if (viewer != null && fromUser) {
                val prevValue = (viewer as? PagerViewer)?.pager?.currentItem ?: -1
                moveToPageIndex(value.roundToInt())
                val newValue = (viewer as? PagerViewer)?.pager?.currentItem ?: -1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
                    ((prevValue > -1 && newValue != prevValue) || viewer !is PagerViewer)
                ) {
                    binding.readerNav.pageSeekbar.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                }
            }
        }

        binding.readerNav.pageSeekbar.setLabelFormatter { value ->
            val pageNumber = (value + 1).roundToInt()
            (viewer as? PagerViewer)?.let {
                if (it.config.doublePages || it.config.splitPages) {
                    if (it.hasExtraPage(value.roundToInt(), viewModel.getCurrentChapter())) {
                        val invertDoublePage = (viewer as? PagerViewer)?.config?.invertDoublePages ?: false
                        return@setLabelFormatter if (!binding.readerNav.pageSeekbar.isRTL.xor(invertDoublePage)) {
                            "$pageNumber-${pageNumber + 1}"
                        } else {
                            "${pageNumber + 1}-$pageNumber"
                        }
                    }
                }
            }
            pageNumber.toString()
        }

        // Set initial visibility
        setMenuVisibility(menuVisible, false)
        binding.chaptersSheet.chaptersBottomSheet.sheetBehavior?.isHideable = !menuVisible
        if (!menuVisible) binding.chaptersSheet.chaptersBottomSheet.sheetBehavior?.hide()
        binding.chaptersSheet.root.sheetBehavior?.isGestureInsetBottomIgnored = true
        val peek = 50.dpToPx
        lastVis = window.decorView.rootWindowInsetsCompat?.isVisible(statusBars()) ?: false
        var firstPass = true
        binding.readerLayout.doOnApplyWindowInsetsCompat { _, insets, _ ->
            setNavColor(insets)
            val systemInsets = insets.ignoredSystemInsets
            val currentOrientation = resources.configuration.orientation
            val isLandscapeFully = currentOrientation == Configuration.ORIENTATION_LANDSCAPE && preferences.landscapeCutoutBehavior().get() == 1
            val cutOutInsets = if (isLandscapeFully) insets.displayCutout else null
            val vis = insets.isVisible(statusBars())
            val fullscreen = preferences.fullscreen().get() && !isSplitScreen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!firstPass && lastVis != vis && fullscreen) {
                    onVisibilityChange(vis)
                }
                firstPass = false
                lastVis = vis
            }
            wic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            if (!fullscreen && sheetManageNavColor) {
                window.navigationBarColor = getResourceColor(R.attr.colorSurface)
            }
            binding.appBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemInsets.left
                rightMargin = systemInsets.right
            }
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemInsets.top
            }
            binding.chaptersSheet.chaptersBottomSheet.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemInsets.left
                rightMargin = systemInsets.right
                height = 280.dpToPx + systemInsets.bottom
            }
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = cutOutInsets?.safeInsetLeft ?: 0
                rightMargin = cutOutInsets?.safeInsetRight ?: 0
            }
            binding.chaptersSheet.topbarLayout.updatePadding(
                left = cutOutInsets?.safeInsetLeft ?: 0,
                right = cutOutInsets?.safeInsetRight ?: 0,
            )
            binding.chaptersSheet.chapterRecycler.updatePadding(
                left = cutOutInsets?.safeInsetLeft ?: 0,
                right = cutOutInsets?.safeInsetRight ?: 0,
            )
            binding.navLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = 12.dpToPx + max(systemInsets.left, cutOutInsets?.safeInsetLeft ?: 0)
                rightMargin = 12.dpToPx + max(systemInsets.right, cutOutInsets?.safeInsetRight ?: 0)
            }
            binding.chaptersSheet.root.sheetBehavior?.peekHeight =
                peek + if (fullscreen) {
                insets.getBottomGestureInsets()
            } else {
                val rootInsets = binding.root.rootWindowInsetsCompat ?: insets
                max(
                    0,
                    (rootInsets.getBottomGestureInsets()) -
                        rootInsets.getInsetsIgnoringVisibility(systemBars()).bottom,
                )
            }
            binding.chaptersSheet.chapterRecycler.updatePaddingRelative(bottom = systemInsets.bottom)
            binding.viewerContainer.requestLayout()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            binding.readerLayout.setOnSystemUiVisibilityChangeListener {
                if (preferences.fullscreen().get()) {
                    onVisibilityChange((it and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0)
                }
            }
        }
    }

    private fun loadAdjacentChapter(rightButton: Boolean) {
        if (isLoading) {
            return
        }
        isScrollingThroughPagesOrChapters = true
        lifecycleScope.launch {
            val getNextChapter = (viewer is R2LPagerViewer).xor(rightButton)
            val adjChapter = viewModel.adjacentChapter(getNextChapter)
            if (adjChapter != null) {
                if (rightButton) {
                    binding.readerNav.rightChapter.isInvisible = true
                    binding.readerNav.rightProgress.isVisible = true
                } else {
                    binding.readerNav.leftChapter.isInvisible = true
                    binding.readerNav.leftProgress.isVisible = true
                }
                loadChapter(adjChapter)
            } else {
                toast(
                    if (getNextChapter) {
                        R.string.theres_no_next_chapter
                    } else {
                        R.string.theres_no_previous_chapter
                    },
                )
            }
        }
    }

    suspend fun loadChapter(chapter: Chapter) {
        if (mode == ReaderMode.GALLERY) {
            return
        }
        loadChapter(ReaderChapter.MangaChapter(chapter))
    }

    suspend fun loadGallery(gallery: GalleryBo) {
        if (mode == ReaderMode.MANGA) {
            return
        }
        loadChapter(ReaderChapter.Gallery(gallery))
    }

    private suspend fun loadChapter(chapter: ReaderChapter) {
        val lastPage = viewModel.loadChapter(chapter) ?: return
        scope.launchUI {
            moveToPageIndex(lastPage, false, chapterChange = true)
        }
        refreshChapters()
    }

    fun setNavColor(insets: WindowInsetsCompat) {
        sheetManageNavColor = when {
            isSplitScreen -> {
                window.statusBarColor = getResourceColor(R.attr.colorPrimaryVariant)
                window.navigationBarColor = getResourceColor(R.attr.colorPrimaryVariant)
                false
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 -> {
                // basically if in landscape on a phone
                // For lollipop, draw opaque nav bar
                window.navigationBarColor = when {
                    insets.hasSideNavBar() -> Color.BLACK
                    isInNightMode() -> {
                        ColorUtils.setAlphaComponent(
                            getResourceColor(R.attr.colorPrimaryVariant),
                            179,
                        )
                    }
                    else -> Color.argb(179, 0, 0, 0)
                }
                !insets.hasSideNavBar()
            }
            insets.isBottomTappable() -> {
                window.navigationBarColor = Color.TRANSPARENT
                false
            }
            insets.hasSideNavBar() -> {
                window.navigationBarColor = getResourceColor(R.attr.colorSurface)
                false
            }
            // if in portrait with 2/3 button mode, translucent nav bar
            else -> {
                true
            }
        }
    }

    private fun showPageLayoutMenu() {
        with(binding.chaptersSheet.doublePage) {
            val config = (viewer as? PagerViewer)?.config
            val selectedId = when {
                config?.doublePages == true -> PageLayout.DOUBLE_PAGES
                config?.splitPages == true -> PageLayout.SPLIT_PAGES
                else -> PageLayout.SINGLE_PAGE
            }
            popupMenu(
                items = listOf(
                    PageLayout.SINGLE_PAGE,
                    PageLayout.DOUBLE_PAGES,
                    PageLayout.SPLIT_PAGES,
                ).map { it.value to it.stringRes },
                selectedItemId = selectedId.value,
            ) {
                val newLayout = PageLayout.fromPreference(itemId)

                if (preferences.pageLayout().get() == PageLayout.AUTOMATIC.value) {
                    (viewer as? PagerViewer)?.config?.let { config ->
                        config.doublePages = newLayout == PageLayout.DOUBLE_PAGES
                        if (newLayout == PageLayout.SINGLE_PAGE) {
                            preferences.automaticSplitsPage().set(false)
                        } else if (newLayout == PageLayout.SPLIT_PAGES) {
                            preferences.automaticSplitsPage().set(true)
                        }
                        reloadChapters(config.doublePages, true)
                    }
                } else {
                    preferences.pageLayout().set(newLayout.value)
                }
            }
        }
    }

    fun hideMenu() {
        if (menuVisible && !isScrollingThroughPagesOrChapters) {
            setMenuVisibility(false)
        }
    }

    /**
     * Sets the visibility of the menu according to [visible] and with an optional parameter to
     * [animate] the views.
     */
    private fun setMenuVisibility(visible: Boolean, animate: Boolean = true) {
        val oldVisibility = menuVisible
        menuVisible = visible
        if (visible) coroutine?.cancel()
        binding.viewerContainer.requestLayout()
        if (visible) {
            snackbar?.dismiss()
            wic.show(systemBars())
            binding.appBar.isVisible = true

            if (binding.chaptersSheet.chaptersBottomSheet.sheetBehavior.isExpanded()) {
                binding.chaptersSheet.chaptersBottomSheet.sheetBehavior?.isHideable = false
            }
            if (!binding.chaptersSheet.chaptersBottomSheet.sheetBehavior.isExpanded() && sheetManageNavColor) {
                window.navigationBarColor = Color.TRANSPARENT
            }
            if (animate && oldVisibility != menuVisible) {
                if (!menuTemporarilyVisible) {
                    val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_top)
                    toolbarAnimation.doOnStart {
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    }
                    toolbarAnimation.doOnEnd { delayTitleScroll() }
                    binding.appBar.startAnimation(toolbarAnimation)
                } else {
                    delayTitleScroll()
                }
                binding.chaptersSheet.chaptersBottomSheet.sheetBehavior?.collapse()
            }

            binding.toolbar.menu.findItem(R.id.action_auto_play)?.setOnMenuItemClickListener {
                autoPlayTimer?.cancelTickAndProgress()

                val dialog = AutoPlayDialogFragment()
                dialog.setPositiveListener {
                    autoPlayTimer?.cancelTickAndProgress()
                    autoPlayTimer = AutoPlayTimer(
                        2 * 60 * 60 * 1000,
                        10,
                        max = it,
                        progressBar = binding.autoPlayProgressBar,
                    ).apply {
                        nextPageFun = {
                            viewer?.moveToNext()
                        }
                    }
                    autoPlayTimer?.startTickAndProgress()
                    setMenuVisibility(false)
                }
                dialog.show(supportFragmentManager, "AutoPlayDialog")
                true
            }
            autoPlayTimer?.cancelTickAndProgress()
        } else {
            if (preferences.fullscreen().get()) {
                wic.hide(systemBars())
                wic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }

            if (animate && binding.appBar.isVisible) {
                val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_top)
                toolbarAnimation.doOnEnd {
                    binding.appBar.isVisible = false
                    stopTitleScroll()
                }
                binding.appBar.startAnimation(toolbarAnimation)
                binding.chaptersSheet.chaptersBottomSheet.sheetBehavior?.isHideable = true
                binding.chaptersSheet.chaptersBottomSheet.sheetBehavior?.hide()
            } else if (!animate) {
                binding.appBar.isVisible = false
                stopTitleScroll()
            }
        }
        menuTemporarilyVisible = false
    }

    /**
     * Called from the view model when a manga is ready. Used to instantiate the appropriate viewer
     * and the binding.toolbar title.
     */
    private fun setManga(manga: Manga) {
        val prevViewer = viewer
        val noDefault = manga.viewer_flags == -1
        val mangaViewer = viewModel.getMangaReadingMode()
        val newViewer = when (mangaViewer) {
            ReadingModeType.LEFT_TO_RIGHT.flagValue -> L2RPagerViewer(this)
            ReadingModeType.VERTICAL.flagValue -> VerticalPagerViewer(this)
            ReadingModeType.WEBTOON.flagValue -> WebtoonViewer(this)
            ReadingModeType.CONTINUOUS_VERTICAL.flagValue -> WebtoonViewer(this, hasMargins = true)
            else -> R2LPagerViewer(this)
        }

        if (noDefault && viewModel.manga?.readingModeType!! > 0 &&
            viewModel.manga?.readingModeType!! != preferences.defaultReadingMode()
        ) {
            snackbar = binding.readerLayout.snack(
                getString(
                    R.string.reading_,
                    getString(
                        when (mangaViewer) {
                            ReadingModeType.RIGHT_TO_LEFT.flagValue -> R.string.right_to_left_viewer
                            ReadingModeType.VERTICAL.flagValue -> R.string.vertical_viewer
                            ReadingModeType.WEBTOON.flagValue -> R.string.webtoon_style
                            else -> R.string.left_to_right_viewer
                        },
                    ).lowercase(Locale.getDefault()),
                ),
                4000,
            ) {
                setAction(R.string.use_default) {
                    viewModel.setMangaReadingMode(0)
                }
            }
        }

        if (window.sharedElementEnterTransition is MaterialContainerTransform &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {
            // Wait until transition is complete to avoid crash on API 26
            window.sharedElementEnterTransition.addListener(
                onEnd = { setOrientation(viewModel.getMangaOrientationType()) },
            )
        } else {
            setOrientation(viewModel.getMangaOrientationType())
        }

        // Destroy previous viewer if there was one
        if (prevViewer != null) {
            prevViewer.destroy()
            binding.viewerContainer.removeAllViews()
        }
        viewer = newViewer
        binding.viewerContainer.addView(newViewer.getView())

        viewer?.setOnPageChangedListener(
            object : BaseViewer.OnPageChangedListener {
                override fun onPageChanged(curPage: SlidePageHolder?) {
                    if (curPage == null) {
                        return
                    }
                    val viewerCurPage = viewer?.currentPage() ?: return

                    // 页面切换时保存上一页的动画（如果有未保存的修改）
                    saveTimePoint()

                    // 离开上一页：恢复其平移限制（避免旧页停留在放松状态）
                    currentAnimationEngine?.restorePanLimit()

                    // 获取当前图片 ID 和对应的 holder
                    // 如果 viewer 当前页面与 curPage 不同，且 viewer 是 PagerViewer，
                    // 尝试从 viewer 获取对应页面的 holder（用于处理 PagerViewer 的特殊情况）
                    val (imageId, curHolder) = when {
                        viewerCurPage.id != curPage.page.id && viewer is PagerViewer -> {
                            val tempHolder = (viewer as PagerViewer).getPageHolder(viewerCurPage)
                            if (tempHolder is SlidePageHolder) {
                                tempHolder.page.id.getImageId() to tempHolder
                            } else {
                                curPage.page.id.getImageId() to curPage
                            }
                        }
                        else -> curPage.page.id.getImageId() to curPage
                    }

                    // 保存当前页面引用
                    currentPageHolder = curHolder
                    currentImageView = curHolder.getImageView()

                    if (imageId <= 0L || currentImageView == null) {
                        return
                    }

                    val engine = curHolder.animationEngine ?: run {
                        curHolder.animationEngine = SlideAnimationEngine(currentImageView, SlideAnimationPath.empty()).apply {
                            onAnimationComplete = {
                                curHolder.onAnimationComplete?.invoke()
                            }
                        }
                        curHolder.animationEngine!!
                    }

                    // 清理之前的回调
                    currentAnimationEngine?.onFrameUpdate = null
                    currentAnimationEngine = engine

                    loadAnimationJob?.cancel("Launch new job")

                    loadAnimationJob = scope.launchIO {
                        // 停止引擎并重置播放位置
                        engine.stop()
                        engine.elapsedTime = 0L

                        // 加载该图片已保存的动画序列（如果存在）
                        animationSaveManager.load(imageId)?.let {
                            currentAnimationEngine?.animationPath = it
                            Timber.e("imageId=$imageId, animationPath=$currentAnimationEngine?.animationPath, currentAnimationEngine=$currentAnimationEngine")
                        } ?: run {
                            Timber.e("imageId=$imageId, load is null, ")
                        }

                        curHolder.isAnimationEnabled = true

                        // 设置动画完成回调（可选）
                        curHolder.onAnimationComplete = {
                            // 动画完成后的操作，例如自动翻页
                            // holder.viewer.moveToNext()
                        }
                        animationSaveManager.setCurrentAnimation(imageId, currentPageHolder?.animationPath)

                        withUIContext {
                            binding.keyFrameTimeline.keyFrameTimelineBody.adapter = object : KeyFrameTimelineView.TimelineAdapter {
                                override fun getDuration() = currentAnimationPath?.durationMs ?: DEFAULT_DURATION

                                override fun getCurrentPosition() = currentAnimationEngine?.elapsedTime ?: 0L

                                override fun getKeyFrames() = currentAnimationPath?.keyFrames ?: listOf()

                                override fun onSeekTo(position: Long) {
                                    currentAnimationEngine?.apply {
                                        this.elapsedTime = position
                                        this.animationPath.getStateAtTime(position)
                                            .apply(this::applyState)
                                    }
                                    // 自动选中最近的关键帧
                                    val keyFrames = currentAnimationPath?.keyFrames ?: return
                                    val nearestIndex = keyFrames.indices
                                        .minByOrNull { kotlin.math.abs(keyFrames[it].timeMs - position) }
                                        ?.takeIf { kotlin.math.abs(keyFrames[it].timeMs - position) < 100 }
                                    selectedKeyFrameIndex = nearestIndex
                                    binding.keyFrameTimeline.keyFrameTimelineBody.selectKeyFrame(
                                        nearestIndex?.let { keyFrames[it].timeMs },
                                    )
                                    updateTimeDisplay()
                                    if (nearestIndex != null) updateKeyframeEditSheetIfOpen(nearestIndex)
                                }

                                override fun onKeyFrameSelected(index: Int) {
                                    val keyFrames = currentAnimationPath?.keyFrames ?: return
                                    val keyFrame = keyFrames.getOrNull(index) ?: return
                                    selectedKeyFrameIndex = index
                                    currentAnimationEngine?.apply {
                                        this.elapsedTime = keyFrame.timeMs
                                        this.animationPath.getStateAtTime(keyFrame.timeMs)
                                            .apply(this::applyState)
                                    }
                                    updateTimeDisplay()
                                    updateKeyframeEditSheetIfOpen(index)
                                }

                                override fun onKeyFrameLongPress(index: Int) {
                                    selectedKeyFrameIndex = index
                                    openKeyframeEditSheet()
                                }

                                override fun onKeyFrameTimeChanged(index: Int, newTimeMs: Long) {
                                    val path = currentAnimationPath ?: return
                                    val oldFrame = path.keyFrames.getOrNull(index) ?: return
                                    val newKeyFrames = path.keyFrames.toMutableList()
                                    newKeyFrames[index] = eu.kanade.tachiyomi.ui.reader.slide.engine.KeyFrame(newTimeMs, oldFrame.state)
                                    newKeyFrames.sortBy { it.timeMs }

                                    val newPath = path.copy(keyFrames = newKeyFrames)
                                    currentAnimationEngine?.animationPath = newPath
                                    currentPageHolder?.animationPath = newPath
                                    syncAnimationToSaveManager()
                                    binding.keyFrameTimeline.keyFrameTimelineBody.invalidate()

                                    // 排序后 index 可能变化，重新查找
                                    val newIndex = newKeyFrames.indexOfFirst { it.timeMs == newTimeMs }
                                    selectedKeyFrameIndex = newIndex
                                    updateTimeDisplay()
                                    if (newIndex >= 0) updateKeyframeEditSheetIfOpen(newIndex)
                                    onKeyFrameTouchSelected(newIndex)
                                }

                                override fun onKeyFrameTouchSelected(index: Int?) {
                                    val tv = binding.keyFrameTimeline.tvSelectedKeyframe
                                    if (index != null) {
                                        val keyFrame = currentAnimationPath?.keyFrames?.getOrNull(index)
                                        if (keyFrame != null) {
                                            tv.text = "已选中: ${(keyFrame.timeMs / 1000f).format(1)}s"
                                            tv.visibility = android.view.View.VISIBLE
                                        }
                                    } else {
                                        tv.visibility = android.view.View.GONE
                                    }
                                }
                            }

                            // 帧更新回调（播放时同步时间轴）
                            engine.onFrameUpdate = { elapsedTimeMs ->
                                val now = System.currentTimeMillis()
                                if (now - lastTimelineUpdateTime >= TIMELINE_UPDATE_INTERVAL_MS) {
                                    lastTimelineUpdateTime = now
                                    binding.keyFrameTimeline.keyFrameTimelineBody.seekToTime(elapsedTimeMs)
                                    updateTimeDisplay()
                                }

                                // 动画播放完成时恢复播放按钮图标
                                val path = currentAnimationPath
                                if (path != null && elapsedTimeMs >= path.durationMs) {
                                    isPlaying = false
                                    binding.keyFrameTimeline.btnPlayPause.setImageResource(R.drawable.ic_play_arrow_24dp)
                                }
                            }

                            // 页面切换时重置播放器状态到 0s
                            isPlaying = false
                            binding.keyFrameTimeline.btnPlayPause.setImageResource(R.drawable.ic_play_arrow_24dp)
                            binding.keyFrameTimeline.keyFrameTimelineBody.seekToTime(0L)
                            selectedKeyFrameIndex = null
                            updateTimeDisplay()
                        }
                    }
                }
            },
        )

        if (newViewer is R2LPagerViewer) {
            binding.readerNav.leftChapter.compatToolTipText = getString(R.string.next_chapter)
            binding.readerNav.rightChapter.compatToolTipText = getString(R.string.previous_chapter)
        } else {
            binding.readerNav.leftChapter.compatToolTipText = getString(R.string.previous_chapter)
            binding.readerNav.rightChapter.compatToolTipText = getString(R.string.next_chapter)
        }

        if (newViewer is PagerViewer) {
            newViewer.config.hingeGapSize = hingeGapSize
            if (preferences.pageLayout().get() == PageLayout.AUTOMATIC.value) {
                setDoublePageMode(newViewer)
            }
            lastShiftDoubleState?.let { newViewer.config.shiftDoublePage = it }
        }

        binding.navigationOverlay.isLTR = viewer !is R2LPagerViewer
        binding.viewerContainer.setBackgroundColor(
            if (viewer is WebtoonViewer) {
                Color.BLACK
            } else {
                getResourceColor(R.attr.background)
            },
        )

        supportActionBar?.title = manga.title

        binding.readerNav.pageSeekbar.isRTL = newViewer is R2LPagerViewer

        binding.pleaseWait.isVisible = true
        binding.pleaseWait.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_long))
        invalidateOptionsMenu()
        updateCropBordersShortcut()
        updateBottomShortcuts()
        val viewerMode = ReadingModeType.fromPreference(viewModel.state.value.manga?.readingModeType ?: 0)
        binding.chaptersSheet.readingMode.setImageResource(viewerMode.iconRes)
        startPostponedEnterTransition()
    }

    override fun onPause() {
        viewModel.saveCurrentChapterReadingProgress()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setReadStartTime()
    }

    fun reloadChapters(doublePages: Boolean, force: Boolean = false) {
        val pViewer = viewer as? PagerViewer ?: return
        pViewer.updateShifting()
        if (!force && pViewer.config.autoDoublePages) {
            setDoublePageMode(pViewer)
        } else {
            pViewer.config.doublePages = doublePages
            if (pViewer.config.autoDoublePages) {
                pViewer.config.splitPages = preferences.automaticSplitsPage().get() && !pViewer.config.doublePages
            }
        }
        if (doublePages) {
            // If we're moving from single to double, we want the current page to be the first page
            val currentIndex = binding.readerNav.pageSeekbar.value.roundToInt()
            viewModel.getCurrentChapter()?.requestedPage = currentIndex
            pViewer.hasMoved = false
            pViewer.config.shiftDoublePage = shouldShiftDoublePages(currentIndex)
        }
        viewModel.state.value.viewerChapters?.let {
            pViewer.setChaptersDoubleShift(it)
        }
        invalidateOptionsMenu()
    }

    private fun shouldShiftDoublePages(currentIndex: Int): Boolean {
        val currentChapter = viewModel.getCurrentChapter()
        return (
            currentIndex +
                (currentChapter?.pages?.take(currentIndex)?.count { it.alonePage } ?: 0)
            ) % 2 != 0
    }

    /**
     * Called from the view model whenever a new [viewerChapters] have been set. It delegates the
     * method to the current viewer, but also set the subtitle on the binding.toolbar.
     */
    fun setChapters(viewerChapters: ViewerChapters) {
        binding.pleaseWait.clearAnimation()
        binding.pleaseWait.isVisible = false
        if (indexChapterToShift != null && indexPageToShift != null) {
            viewerChapters.currChapter.pages?.find { it.index == indexPageToShift && it.chapter.chapterId == indexChapterToShift }
                ?.let {
                    (viewer as? PagerViewer)?.updateShifting(it)
                }
            indexChapterToShift = null
            indexPageToShift = null
        }
        val currentChapterPageCount = viewerChapters.currChapter.pages?.size ?: 1
        binding.readerNav.root.visibility = when {
            currentChapterPageCount == 1 -> View.GONE
            binding.chaptersSheet.root.sheetBehavior.isCollapsed() -> View.VISIBLE
            else -> View.INVISIBLE
        }
        // 时间轴只在画廊模式下显示，且与导航栏保持一致的可见性
        binding.keyFrameTimeline.root.visibility = if (mode == ReaderMode.GALLERY) {
            binding.readerNav.root.visibility
        } else {
            View.GONE
        }
        if (lastShiftDoubleState == null) {
            manuallyShiftedPages = false
        }
        lastShiftDoubleState = null
        viewer?.setChapters(viewerChapters)
        intentPageNumber?.let { moveToPageIndex(it) }
        intentPageNumber = null

        when (val curChapter = viewerChapters.currChapter) {
            is ReaderChapter.MangaChapter -> {
                val chapter = curChapter.chapter
                binding.toolbar.subtitle =
                    chapter.preferredChapterName(this, viewModel.manga!!, preferences)
            }

            is ReaderChapter.Gallery -> {
                binding.toolbar.subtitle =
                    "图片浏览模式"
            }
        }

        listOfNotNull(getTitleTextView(), getSubtitleTextView()).forEach { textView ->
            textView.ellipsize = TextUtils.TruncateAt.MARQUEE
            textView.marqueeRepeatLimit = -1
            textView.isSingleLine = true
            textView.isFocusable = true
            textView.isFocusableInTouchMode = true
            textView.isHorizontalFadingEdgeEnabled = true
            textView.setFadingEdgeLength(16.dpToPx)
            textView.setHorizontallyScrolling(true)
        }

        if (viewerChapters.nextChapter == null && viewerChapters.prevChapter == null) {
            binding.readerNav.leftChapter.isVisible = false
            binding.readerNav.rightChapter.isVisible = false
        } else if (viewer is R2LPagerViewer) {
            binding.readerNav.leftChapter.alpha = if (viewerChapters.nextChapter != null) 1f else 0.5f
            binding.readerNav.rightChapter.alpha = if (viewerChapters.prevChapter != null) 1f else 0.5f
        } else {
            binding.readerNav.rightChapter.alpha = if (viewerChapters.nextChapter != null) 1f else 0.5f
            binding.readerNav.leftChapter.alpha = if (viewerChapters.prevChapter != null) 1f else 0.5f
        }
        if (didTransitionFromChapter) {
            MainActivity.chapterIdToExitTo = viewerChapters.currChapter.chapterId
        }
    }

    private fun getTitleTextView(): TextView? = getTextViewsWithText(binding.toolbar.title)
    private fun getSubtitleTextView(): TextView? = getTextViewsWithText(binding.toolbar.subtitle)

    private fun getTextViewsWithText(text: CharSequence?): TextView? {
        if (text.isNullOrBlank()) return null
        val viewTopComparator = Comparator<View> { view1, view2 -> view1.top - view2.top }
        val textViews = binding.toolbar.children.filterIsInstance<TextView>()
            .filter { TextUtils.equals(it.text, text) }.toList()
        return if (textViews.isEmpty()) null else Collections.max(textViews, viewTopComparator)
    }

    private fun delayTitleScroll() {
        val list = listOfNotNull(getTitleTextView(), getSubtitleTextView())
        if (list.isNotEmpty()) {
            scope.launchUI {
                delay(1000)
                if (menuVisible) {
                    list.forEach { it.isSelected = true }
                }
            }
        }
    }

    private fun stopTitleScroll() =
        listOfNotNull(getTitleTextView(), getSubtitleTextView()).forEach { it.isSelected = false }

    /**
     * Called from the view model if the initial load couldn't load the pages of the chapter. In
     * this case the activity is closed and a toast is shown to the user.
     */
    private fun setInitialChapterError(error: Throwable) {
        Timber.e(error)
        finish()
        toast(error.message)
    }

    /**
     * Called from the view model whenever it's loading the next or previous chapter. It shows or
     * dismisses a non-cancellable dialog to prevent user interaction according to the value of
     * [show]. This is only used when the next/previous buttons on the binding.toolbar are clicked; the
     * other cases are handled with chapter transitions on the viewers and chapter preloading.
     */
    private fun setProgressDialog(show: Boolean) {
        if (!show) {
            binding.readerNav.leftChapter.isVisible = true
            binding.readerNav.rightChapter.isVisible = true

            binding.readerNav.leftProgress.isVisible = false
            binding.readerNav.rightProgress.isVisible = false
            binding.chaptersSheet.root.resetChapter()
        }
        if (show) {
            isLoading = true
        } else {
            scope.launchIO {
                delay(100)
                isLoading = false
            }
        }
    }

    /**
     * Moves the viewer to the given page [index]. It does nothing if the viewer is null or the
     * page is not found.
     */
    private fun moveToPageIndex(index: Int, animated: Boolean = true, chapterChange: Boolean = false) {
        val viewer = viewer ?: return
        val currentChapter = viewModel.getCurrentChapter() ?: return
        val page = currentChapter.pages?.getOrNull(index) ?: return
        viewer.moveToPage(page, animated)
        if (chapterChange) {
            isScrollingThroughPagesOrChapters = false
        }
    }

    private fun refreshChapters() {
        binding.chaptersSheet.chaptersBottomSheet.refreshList()
    }

    /**
     * Called from the viewer whenever a [page] is marked as active. It updates the values of the
     * bottom menu and delegates the change to the view model.
     */
    @SuppressLint("SetTextI18n")
    fun onPageSelected(page: ReaderPage, hasExtraPage: Boolean) {
        viewModel.onPageSelected(page, hasExtraPage)
        val pages = page.chapter.pages ?: return

        val currentPage = if (hasExtraPage) {
            val invertDoublePage = (viewer as? PagerViewer)?.config?.invertDoublePages ?: false
            if (!binding.readerNav.pageSeekbar.isRTL.xor(invertDoublePage)) {
                "${page.number}-${page.number + 1}"
            } else {
                "${page.number + 1}-${page.number}"
            }
        } else {
            "${page.number}${if (page.firstHalf == false) "*" else ""}"
        }

        val totalPages = pages.size.toString()
        if (hingeGapSize > 0) {
            binding.pageNumber.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                marginStart = (binding.root.width) / 2 + hingeGapSize
            }
        }
        binding.pageNumber.text = if (resources.isLTR) "$currentPage/$totalPages" else "$totalPages/$currentPage"
        if (viewer is R2LPagerViewer) {
            binding.readerNav.rightPageText.text = currentPage
            binding.readerNav.leftPageText.text = totalPages
        } else {
            binding.readerNav.leftPageText.text = currentPage
            binding.readerNav.rightPageText.text = totalPages
        }
        if (binding.chaptersSheet.chaptersBottomSheet.selectedChapterId != page.chapter.chapterId) {
            binding.chaptersSheet.chaptersBottomSheet.refreshList()
        }
        // Set seekbar progress
        binding.readerNav.pageSeekbar.valueTo = max(pages.lastIndex.toFloat(), 1f)
        val progress = page.index + if (hasExtraPage) 1 else 0
        // For a double page, show the last 2 pages as if it was the final part of the seekbar
        binding.readerNav.pageSeekbar.value = (if (progress == pages.lastIndex) progress else page.index).toFloat()
    }

    /**
     * Called from the viewer whenever a [page] is long clicked. A bottom sheet with a list of
     * actions to perform is shown.
     */
    fun onPageLongTap(page: ReaderPage, extraPage: ReaderPage? = null) {
        val items = if (extraPage != null) {
            listOf(
                MaterialMenuSheet.MenuSheetItem(
                    100,
                    R.drawable.ic_bookmark_24dp,
                    R.string.mark_second_page,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    3,
                    R.drawable.ic_outline_share_24dp,
                    R.string.share_second_page,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    4,
                    R.drawable.ic_outline_save_24dp,
                    R.string.save_second_page,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    5,
                    R.drawable.ic_outline_photo_24dp,
                    R.string.set_second_page_as_cover,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    101,
                    R.drawable.ic_bookmark_24dp,
                    R.string.mark_first_page,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    0,
                    R.drawable.ic_share_24dp,
                    R.string.share_first_page,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    1,
                    R.drawable.ic_save_24dp,
                    R.string.save_first_page,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    2,
                    R.drawable.ic_photo_24dp,
                    R.string.set_first_page_as_cover,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    6,
                    R.drawable.ic_share_all_outline_24dp,
                    R.string.share_combined_pages,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    7,
                    R.drawable.ic_save_all_outline_24dp,
                    R.string.save_combined_pages,
                ),
            )
        } else {
            listOf(
                MaterialMenuSheet.MenuSheetItem(
                    102,
                    R.drawable.ic_bookmark_24dp,
                    R.string.mark_page,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    0,
                    R.drawable.ic_share_24dp,
                    R.string.share,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    1,
                    R.drawable.ic_save_24dp,
                    R.string.save,
                ),
            ) + when (mode) {
                ReaderMode.MANGA -> listOf(
                    MaterialMenuSheet.MenuSheetItem(
                        2,
                        R.drawable.ic_photo_24dp,
                        R.string.set_as_cover,
                    ),
                )

                else -> emptyList()
            }
        }
        MaterialMenuSheet(this, items) { _, item ->
            when (item) {
                0 -> shareImage(page)
                1 -> saveImage(page)
                2 -> showSetCoverPrompt(page)
                3 -> extraPage?.let { shareImage(it) }
                4 -> extraPage?.let { saveImage(it) }
                5 -> extraPage?.let { showSetCoverPrompt(it) }
                6, 7 -> extraPage?.let { secondPage ->
                    (viewer as? PagerViewer)?.let { viewer ->
                        val isLTR = (viewer !is R2LPagerViewer).xor(viewer.config.invertDoublePages)
                        val bg = ThemeUtil.readerBackgroundColor(viewer.config.readerTheme)
                        if (item == 6) {
                            viewModel.shareImages(page, secondPage, isLTR, bg)
                        } else {
                            viewModel.saveImages(page, secondPage, isLTR, bg)
                        }
                    }
                }
                102, 100 -> showMarkDialog(page)
                101 -> extraPage?.let { showMarkDialog(it) }
            }
            true
        }.show()
        if (binding.chaptersSheet.root.sheetBehavior.isExpanded()) {
            binding.chaptersSheet.root.sheetBehavior?.collapse()
        }
    }

    /**
     * Called from the viewer when the given [chapter] should be preloaded. It should be called when
     * the viewer is reaching the beginning or end of a chapter or the transition page is active.
     */
    fun requestPreloadChapter(chapter: ReaderChapter) {
        lifecycleScope.launch {
            viewModel.preloadChapter(chapter)
        }
    }

    /**
     * Called from the viewer to toggle the visibility of the menu. It's implemented on the
     * viewer because each one implements its own touch and key events.
     */
    fun toggleMenu() {
        setMenuVisibility(!menuVisible)
    }

    /**
     * Called from the viewer to show the menu.
     */
    fun showMenu() {
        if (!menuVisible) {
            setMenuVisibility(true)
        }
    }

    /**
     * Called from the page sheet. It delegates the call to the view model to do some IO, which
     * will call [onShareImageResult] with the path the image was saved on when it's ready.
     */
    private fun shareImage(page: ReaderPage) {
        viewModel.shareImage(page)
    }

    private fun showMarkDialog(page: ReaderPage) {
        scope.launchUI {
            val currentImage = withIOContext {
                page.tryGetImage()
            } ?: return@launchUI
            val tagSettingsSheet = TagSettingsSheet(this@ReaderActivity, currentImage) {
                startActivity(
                    MainActivity.newIntentJumpGallerySearch(this@ReaderActivity, it.tagId),
                )
                finish()
            }
            tagSettingsSheet.show()
        }
    }

    private fun showSetCoverPrompt(page: ReaderPage) {
        if (page.status != Page.State.READY) return

        materialAlertDialog()
            .setMessage(R.string.use_image_as_cover)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                setAsCover(page)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Called from the view model when a page is ready to be shared. It shows Android's default
     * sharing tool.
     */
    private fun onShareImageResult(file: File, page: ReaderPage, secondPage: ReaderPage? = null) {
        val manga = viewModel.manga ?: return
        var text = ""
        when (val curChapter = page.chapter) {
            is ReaderChapter.MangaChapter -> {
                val chapter = curChapter.chapter

                val decimalFormat =
                    DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })

                val pageNumber = if (secondPage != null) {
                    getString(
                        R.string.pages_,
                        if (resources.isLTR) "${page.number}-${page.number + 1}" else "${page.number + 1}-${page.number}",
                    )
                } else {
                    getString(R.string.page_, page.number)
                }
                text = "${manga.title}: ${
                if (chapter.isRecognizedNumber) {
                    getString(R.string.chapter_, decimalFormat.format(chapter.chapter_number))
                } else {
                    chapter.preferredChapterName(this, manga, preferences)
                }
                }, $pageNumber"
            }

            is ReaderChapter.Gallery -> {
                text = "这是gallery分享测试text"
            }
        }

        val stream = file.getUriCompat(this)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, stream)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            clipData = ClipData.newRawUri(null, stream)
            type = "image/*"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        val chapterUrl = viewModel.getChapterUrl() ?: return
        outContent.webUri = Uri.parse(chapterUrl)
    }

    /**
     * Called from the page sheet. It delegates saving the image of the given [page] on external
     * storage to the viewModel.
     */
    private fun saveImage(page: ReaderPage) {
        viewModel.saveImage(page)
    }

    /**
     * Called from the view model when a page is saved or fails. It shows a message or logs the
     * event depending on the [result].
     */
    private fun onSaveImageResult(result: ReaderViewModel.SaveImageResult) {
        when (result) {
            is ReaderViewModel.SaveImageResult.Success -> {
                toast(R.string.picture_saved)
            }
            is ReaderViewModel.SaveImageResult.Error -> {
                Timber.e(result.error)
            }
        }
    }

    /**
     * Called from the page sheet. It delegates setting the image of the given [page] as the
     * cover to the viewModel.
     */
    private fun setAsCover(page: ReaderPage) {
        viewModel.setAsCover(page)
    }

    /**
     * Called from the view model when a page is set as cover or fails. It shows a different message
     * depending on the [result].
     */
    private fun onSetAsCoverResult(result: ReaderViewModel.SetAsCoverResult) {
        toast(
            when (result) {
                Success -> R.string.cover_updated
                AddToLibraryFirst -> R.string.must_be_in_library_to_edit
                Error -> R.string.failed_to_update_cover
            },
        )
    }

    private fun showTrackingError(errors: List<Pair<TrackService, String?>>) {
        if (errors.isEmpty()) return
        snackbar?.dismiss()
        val errorText = if (errors.size > 1) {
            getString(R.string.failed_to_update_, errors.joinToString(", ") { getString(it.first.nameRes()) })
        } else {
            val (service, errorMessage) = errors.first()
            buildSpannedString {
                if (errorMessage != null) {
                    val icon = contextCompatDrawable(service.getLogo())
                        ?.mutate()
                        ?.run {
                            (this as? BitmapDrawable)?.run {
                                val newBitmap = Bitmap.createBitmap(
                                    intrinsicWidth,
                                    intrinsicHeight,
                                    bitmap.config!!,
                                )
                                val canvas = Canvas(newBitmap)
                                val bgColor = ColorUtils.setAlphaComponent(service.getLogoColor(), 255)
                                canvas.drawColor(bgColor)
                                canvas.drawBitmap(bitmap, 0f, 0f, null)
                                BitmapDrawable(resources, newBitmap)
                            } ?: this
                        }?.apply {
                            val size =
                                resources.getDimension(com.google.android.material.R.dimen.design_snackbar_text_size)
                            val dRatio = intrinsicWidth / intrinsicHeight.toFloat()
                            setBounds(0, 0, (size * dRatio).roundToInt(), size.roundToInt())
                        } ?: return
                    val alignment =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) DynamicDrawableSpan.ALIGN_CENTER else DynamicDrawableSpan.ALIGN_BASELINE
                    inSpans(ImageSpan(icon, alignment)) { append("image") }
                    append(" - $errorMessage")
                }
            }
        }
        snackbar = binding.readerLayout.snack(errorText, 5000)
    }

    private fun onVisibilityChange(visible: Boolean) {
        if (visible && !menuTemporarilyVisible && !menuVisible && !binding.appBar.isVisible) {
            menuTemporarilyVisible = true
            coroutine = scope.launchUI {
                delay(2000)
                if (window.decorView.rootWindowInsetsCompat?.isVisible(statusBars()) == true) {
                    menuTemporarilyVisible = false
                    setMenuVisibility(false)
                }
            }
            if (sheetManageNavColor) {
                window.navigationBarColor =
                    ColorUtils.setAlphaComponent(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 || isInNightMode()) {
                            getResourceColor(R.attr.colorSurface)
                        } else {
                            Color.BLACK
                        },
                        if (binding.root.rootWindowInsetsCompat?.hasSideNavBar() == true) {
                            255
                        } else {
                            179
                        },
                    )
            }
            binding.appBar.isVisible = true
            val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_top)
            toolbarAnimation.doOnStart {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }
            binding.appBar.startAnimation(toolbarAnimation)
        } else if (!visible && (menuTemporarilyVisible || menuVisible)) {
            if (menuTemporarilyVisible && !menuVisible) {
                setMenuVisibility(false)
            }
            coroutine?.cancel()
        }
    }

    /**
     * Sets notch cutout mode to "NEVER", if mobile is in a landscape view
     */
    private fun setCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val currentOrientation = resources.configuration.orientation

            val params = window.attributes
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                params.layoutInDisplayCutoutMode =
                    if (preferences.landscapeCutoutBehavior().get() == 0) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            } else {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun setDoublePageMode(viewer: PagerViewer) {
        val currentOrientation = resources.configuration.orientation
        viewer.config.doublePages = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
        if (viewer.config.autoDoublePages) {
            viewer.config.splitPages = preferences.automaticSplitsPage().get() && !viewer.config.doublePages
        }
    }

    private fun handleIntentAction(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        if (!viewModel.canLoadUrl(uri)) {
            openInBrowser(intent.data!!.toString(), true)
            finishAfterTransition()
            return true
        }
        setMenuVisibility(visible = false, animate = true)
        scope.launch(Dispatchers.IO) {
            try {
                intentPageNumber = viewModel.intentPageNumber(uri)
                viewModel.loadChapterURL(uri)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setInitialChapterError(e)
                }
            }
        }
        return true
    }

    private fun openMangaInBrowser() {
        val source = viewModel.getSource() ?: return
        val chapterUrl = viewModel.getChapterUrl() ?: return

        val intent = WebViewActivity.newIntent(
            applicationContext,
            chapterUrl,
            source.id,
            viewModel.manga!!.title,
        )
        startActivity(intent)
    }

    /**
     * Forces the user preferred [orientation] on the activity.
     */
    fun setOrientation(orientation: Int) {
        val newOrientation = OrientationType.fromPreference(orientation)
        if (newOrientation.flag != requestedOrientation) {
            requestedOrientation = newOrientation.flag
        }
    }

    /**
     * Class that handles the user preferences of the reader.
     */
    private inner class ReaderConfig {

        var showNewChapter = false

        /**
         * Initializes the reader subscriptions.
         */
        init {
            preferences.defaultOrientationType().asFlow()
                .drop(1)
                .onEach {
                    delay(250)
                    setOrientation(viewModel.getMangaOrientationType())
                }
                .launchIn(scope)

            preferences.showPageNumber().asImmediateFlowIn(scope) { setPageNumberVisibility(it) }

            preferences.landscapeCutoutBehavior().asFlow()
                .drop(1)
                .onEach { setCutoutMode() }
                .launchIn(scope)

            preferences.trueColor().asImmediateFlowIn(scope) { setTrueColor(it) }

            preferences.fullscreen().asImmediateFlowIn(scope) { setFullscreen(it) }

            preferences.keepScreenOn().asImmediateFlowIn(scope) { setKeepScreenOn(it) }

            preferences.customBrightness().asImmediateFlowIn(scope) { setCustomBrightness(it) }

            preferences.colorFilter().asImmediateFlowIn(scope) { setColorFilter(it) }

            preferences.colorFilterMode().asImmediateFlowIn(scope) {
                setColorFilter(preferences.colorFilter().get())
            }

            merge(preferences.grayscale().asFlow(), preferences.invertedColors().asFlow())
                .onEach { setLayerPaint(preferences.grayscale().get(), preferences.invertedColors().get()) }
                .launchIn(lifecycleScope)

            preferences.alwaysShowChapterTransition().asImmediateFlowIn(scope) {
                showNewChapter = it
            }

            preferences.pageLayout().asImmediateFlowIn(scope) { setBottomNavButtons(it) }

            preferences.automaticSplitsPage().asFlow()
                .drop(1)
                .onEach {
                    val isPaused = !this@ReaderActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    if (isPaused) {
                        (viewer as? PagerViewer)?.config?.let { config ->
                            reloadChapters(config.doublePages, true)
                        }
                    }
                }
                .launchIn(scope)

            preferences.readerBottomButtons().asImmediateFlowIn(scope) { updateBottomShortcuts() }
        }

        /**
         * Sets the visibility of the bottom page indicator according to [visible].
         */
        private fun setPageNumberVisibility(visible: Boolean) {
            binding.pageNumber.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }

        /**
         * Sets the 32-bit color mode according to [enabled].
         */
        private fun setTrueColor(enabled: Boolean) {
            if (enabled) {
                SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
            } else {
                SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.RGB_565)
            }
        }

        /**
         * Sets the fullscreen reading mode (immersive) according to [enabled].
         */
        private fun setFullscreen(enabled: Boolean) {
            WindowCompat.setDecorFitsSystemWindows(window, !enabled || isSplitScreen)
            wic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            binding.root.rootWindowInsetsCompat?.let { setNavColor(it) }
        }

        /**
         * Sets the keep screen on mode according to [enabled].
         */
        private fun setKeepScreenOn(enabled: Boolean) {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        /**
         * Sets the custom brightness overlay according to [enabled].
         */
        private fun setCustomBrightness(enabled: Boolean) {
            if (enabled) {
                preferences.customBrightnessValue().asFlow()
                    .sample(100)
                    .onEach { setCustomBrightnessValue(it) }
                    .launchIn(scope)
            } else {
                setCustomBrightnessValue(0)
            }
        }

        /**
         * Sets the color filter overlay according to [enabled].
         */
        private fun setColorFilter(enabled: Boolean) {
            if (enabled) {
                preferences.colorFilterValue().asFlow()
                    .sample(100)
                    .onEach { setColorFilterValue(it) }
                    .launchIn(scope)
            } else {
                binding.colorOverlay.isVisible = false
            }
        }

        private fun getCombinedPaint(grayscale: Boolean, invertedColors: Boolean): Paint {
            return Paint().apply {
                colorFilter = ColorMatrixColorFilter(
                    ColorMatrix().apply {
                        if (grayscale) {
                            setSaturation(0f)
                        }
                        if (invertedColors) {
                            postConcat(
                                ColorMatrix(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f,
                                    ),
                                ),
                            )
                        }
                    },
                )
            }
        }

        /**
         * Sets the brightness of the screen. Range is [-75, 100].
         * From -75 to -1 a semi-transparent black view is overlaid with the minimum brightness.
         * From 1 to 100 it sets that value as brightness.
         * 0 sets system brightness and hides the overlay.
         */
        private fun setCustomBrightnessValue(value: Int) {
            // Calculate and set reader brightness.
            val readerBrightness = when {
                value > 0 -> {
                    value / 100f
                }
                value < 0 -> {
                    0.01f
                }
                else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }

            window.attributes = window.attributes.apply { screenBrightness = readerBrightness }

            // Set black overlay visibility.
            if (value < 0) {
                binding.brightnessOverlay.isVisible = true
                val alpha = (abs(value) * 2.56).toInt()
                binding.brightnessOverlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
            } else {
                binding.brightnessOverlay.isVisible = false
            }
        }

        /**
         * Sets the color filter [value].
         */
        private fun setColorFilterValue(value: Int) {
            binding.colorOverlay.isVisible = true
            binding.colorOverlay.setFilterColor(value, preferences.colorFilterMode().get())
        }

        private fun setLayerPaint(grayscale: Boolean, invertedColors: Boolean) {
            val paint = if (grayscale || invertedColors) getCombinedPaint(grayscale, invertedColors) else null
            binding.viewerContainer.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        }
    }

    private fun Long?.getImageId() = this ?: 0L
}
