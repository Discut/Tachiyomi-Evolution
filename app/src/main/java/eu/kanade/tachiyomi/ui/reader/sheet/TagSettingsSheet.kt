package eu.kanade.tachiyomi.ui.reader.sheet

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.core.view.allViews
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ItemAiTagResultBinding
import eu.kanade.tachiyomi.databinding.ReaderTagSettingsSheetBinding
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.UnionImageBO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.snack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference

class TagSettingsSheet(
    private val activity: Activity,
    private val imageBO: IImageBo,
    private val searchTagCallback: ((TagVo) -> Unit)? = null,
) :
    BottomSheetDialog(activity) {

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    val viewModel by lazy { TagSettingsViewModel(activity, imageBO) }

    val binding: ReaderTagSettingsSheetBinding by lazy { createBinding(activity.layoutInflater) }

    private var floatingActionMode: ActionMode? = null
    private var floatingActionModeAnchor: View? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN &&
            floatingActionMode != null &&
            floatingActionModeAnchor != null
        ) {
            finishFloatingActionMode()
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun createBinding(inflater: LayoutInflater): ReaderTagSettingsSheetBinding {
        return ReaderTagSettingsSheetBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        binding.textField.editText?.doOnTextChanged { text, start, before, count ->
            viewModel.searchKey = text.toString()
        }

        /*        binding.tagsNestedScrollView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (binding.tagsNestedScrollView.measuredHeight > activity.window.decorView.measuredHeight / 2) {
                binding.tagsNestedScrollView.layoutParams.height =
                    activity.window.decorView.measuredHeight / 2
            } else {
                binding.tagsNestedScrollView.layoutParams.height = -1
            }
        }*/

        behavior.peekHeight = activity.window.decorView.measuredHeight / 2
        behavior.maxHeight = (activity.window.decorView.measuredHeight / 0.8).toInt()

        initCollection()
        setupAITagging()
    }

    private fun initCollection() {
        scope.launch {
            viewModel.resultTagsFlow.collectLatest { it ->
                binding.tagsGroup.removeAllViews()
                it.forEach {
                    addTagView(it)?.apply {
                        post {
                            this.animate()
                                .alpha(1f)
                                .setDuration(300)
                                .setInterpolator(OvershootInterpolator())
                                .start()
                        }
                    }
                }
                /*TransitionManager.beginDelayedTransition(
                    binding.tagsGroup,
                    AutoTransition().apply {
                        duration = 150
                        interpolator = OvershootInterpolator()
                    },
                )*/
                if (binding.textField.editText?.isFocused == false) {
                    TransitionManager.beginDelayedTransition(
                        binding.chipsLinearLayout,
                        AutoTransition().apply {
                            duration = 300
                            interpolator = OvershootInterpolator()
                        },
                    )
                }
            }
        }
    }

    private fun longClickTag(tagVo: TagVo, view: Chip) {
        finishFloatingActionMode()

        if (tagVo == TagSettingsViewModel.PLUS_TAG) {
            Timber.i("Can't long click plus tag")
            return
        }

        floatingActionModeAnchor = view
        floatingActionMode =
            view.startActionMode(
                FloatingImageTagActionModeCallback(tagVo, view),
                ActionMode.TYPE_FLOATING,
            )
    }

    private fun clickTag(tagVo: TagVo, view: Chip) {
        when {
            tagVo == TagSettingsViewModel.PLUS_TAG -> {
                scope.launchUI {
                    TagNewDialog().show(activity) {
                        scope.launchIO {
                            viewModel.createNewTag(it)
                        }
                    }
                }
            }

            !tagVo.isSelected -> {
                if (imageBO is ImageBO) {
                    scope.launchIO {
                        viewModel.relatedImageAndTag(imageBO, tagVo)
                        viewModel.updateAdoptionState(tagVo.tagValue, true)
                    }
                }
            }

            tagVo.isSelected -> {
                if (imageBO is ImageBO) {
                    scope.launchIO {
                        viewModel.unrelatedImageAndTag(imageBO, tagVo)
                        viewModel.updateAdoptionState(tagVo.tagValue, false)
                    }
                }
            }
        }
    }

    private fun addTagView(tagVo: TagVo): Chip? {
        binding.tagsGroup.children
        if (binding.tagsGroup.allViews.any {
            val innerChip = (it as? Chip) ?: return@any false
            return@any innerChip.tag == tagVo.tagId &&
                innerChip.isSelected == tagVo.isSelected &&
                innerChip.text == tagVo.tagValue
        }
        ) {
            return null
        }
        val chip = Chip(context)
        chip.text = tagVo.tagValue
        chip.tag = tagVo.tagId
        chip.isSelected = tagVo.isSelected
        chip.alpha = 0.2f
        // chip.setTextColor(chipTextColor) //设置文字颜色
        // chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, chipTextSize) //设置文字大小
        // chip.setChipBackgroundColorResource(chipBackgroundColor) //设置背景色，支持color state list
        // chip.chipCornerRadius = chipCornerRadius //设置圆角大小
        // chip.setRippleColorResource(rippleColor) //设置点击波纹颜色
        chip.setOnClickListener { clickTag(tagVo, chip) }

        chip.setOnLongClickListener {
            longClickTag(tagVo, chip)
            true
        }

        binding.tagsGroup.addView(chip)
        return chip
    }

    // ========== AI 智能打标 ==========

    private val aiTagAdapter by lazy {
        AITagResultAdapter(
            tagClickedListener = { result ->
                scope.launchIO {
                    viewModel.adoptAITag(result)
                }
            },
            tagLongClickListener = { result, view ->
                showAITagActionMode(result, view)
            },
        )
    }

    private fun setupAITagging() {
        // 关闭内部子滚动，避免与 BottomSheet 和外层 NestedScrollView 的手势冲突
        binding.tagsNestedScrollView.isNestedScrollingEnabled = false
        binding.aiTagResultsRecycler.isNestedScrollingEnabled = false

        binding.aiTagResultsRecycler.layoutManager = LinearLayoutManager(context)
        binding.aiTagResultsRecycler.adapter = aiTagAdapter

        if (imageBO is UnionImageBO) {
            binding.aiTagButton.isEnabled = false
            binding.aiTagButton.text = "AI打标暂不支持对比视图"
            return
        }

        binding.aiTagButton.setOnClickListener {
            val filePath = imageBO.url
            if (filePath.isBlank()) {
                activity.toast("图片路径无效")
                return@setOnClickListener
            }
            scope.launch {
                try {
                    viewModel.predictTags(filePath)
                } catch (e: Exception) {
                    Timber.e(e, "AI打标失败")
                    activity.toast("AI打标失败: ${e.message}")
                }
            }
        }

        // 观察 AI 状态变化
        scope.launch {
            viewModel.aiPredictState.collectLatest { state ->
                when (state) {
                    AIPredictState.LOADING -> {
                        showAILoading()
                        binding.aiTagButton.isEnabled = false
                        binding.aiTagStatsLayout.visibility = View.GONE
                    }

                    AIPredictState.REFRESHING -> {
                        binding.aiLoadingProgress.visibility = View.VISIBLE
                        binding.aiTagButton.isEnabled = false
                    }

                    AIPredictState.RESULTS -> {
                        binding.aiTagButton.isEnabled = true
                        showAIResults(viewModel.aiPredictResults.value)
                    }

                    AIPredictState.EMPTY -> {
                        showAILoading()
                        activity.toast("未识别到任何标签")
                        binding.aiLoadingProgress.visibility = View.GONE
                        binding.aiTagButton.isEnabled = true
                    }

                    AIPredictState.ERROR -> {
                        binding.aiLoadingProgress.visibility = View.GONE
                        binding.aiTagButton.isEnabled = true
                    }

                    AIPredictState.IDLE -> { // no-op
                        binding.aiTagStatsLayout.visibility = View.GONE
                    }
                }
            }
        }

        // 持续观察 aiPredictResults 变化（过滤/取消过滤/采纳状态同步）
        scope.launch {
            viewModel.aiPredictResults.collectLatest { results ->
                if (viewModel.aiPredictState.value == AIPredictState.RESULTS ||
                    viewModel.aiPredictState.value == AIPredictState.REFRESHING
                ) {
                    aiTagAdapter.submitList(results)
                    updateAIStats()
                }
            }
        }

        // Observe all results for stats count
        scope.launch {
            viewModel.aiAllPredictResults.collectLatest { all ->
                updateAIStats()
            }
        }

        // Observe showFiltered toggle for button text
        scope.launch {
            viewModel.showFiltered.collectLatest { showing ->
                binding.aiFilterToggle.text = if (showing) {
                    "隐藏已过滤"
                } else {
                    "显示已过滤"
                }
            }
        }

        binding.aiFilterToggle.setOnClickListener {
            viewModel.showFiltered.value = !viewModel.showFiltered.value
            viewModel.recomputeAiPredictResults()
            aiTagAdapter.submitList(viewModel.aiPredictResults.value.toList())
        }
    }

    private fun showAILoading() {
        binding.aiLoadingProgress.visibility = View.VISIBLE
        binding.aiTagResultsRecycler.visibility = View.GONE
        binding.aiTagButton.isEnabled = false
    }

    private fun showAIResults(results: List<AIPredictResult>) {
        binding.aiLoadingProgress.visibility = View.GONE
        binding.aiTagButton.isEnabled = true
        binding.aiTagResultsRecycler.visibility = View.VISIBLE
        aiTagAdapter.submitList(results.toList())
        updateAIStats()
    }

    private fun updateAIStats() {
        val all = viewModel.aiAllPredictResults.value
        if (all.isEmpty()) return
        val total = all.size
        val filtered = all.count { it.isFiltered }
        binding.aiTagStatsText.text = "共识别 $total 个标签，已过滤 $filtered 个"
        binding.aiTagStatsLayout.visibility = View.VISIBLE
    }

    private fun onAITagClicked(result: AIPredictResult, itemBinding: ItemAiTagResultBinding) {
        if (imageBO !is ImageBO) return
        scope.launchIO {
            try {
                viewModel.adoptAITag(result)
                scope.launch {
                    itemBinding.progressFill.setBackgroundColor(0x33888888.toInt())
                    itemBinding.root.alpha = 0.4f
                    itemBinding.root.isClickable = false
                }
            } catch (e: Exception) {
                Timber.e(e, "采纳AI标签失败")
                scope.launch { activity.toast("采纳失败: ${e.message}") }
            }
        }
    }

    private fun finishFloatingActionMode() {
        floatingActionMode ?: return
        floatingActionMode?.finish()
        floatingActionMode = null
        floatingActionModeAnchor = null
    }

    private fun showAITagActionMode(result: AIPredictResult, view: View) {
        finishFloatingActionMode()
        floatingActionModeAnchor = view
        floatingActionMode = view.startActionMode(
            AITagFilterActionModeCallback(result),
            ActionMode.TYPE_FLOATING,
        )
    }

    inner class AITagFilterActionModeCallback(
        private val result: AIPredictResult,
    ) : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.ai_tag_filter_actions, menu)
            menu?.findItem(R.id.action_filter)?.isVisible = !result.isFiltered && !result.isAdopted
            menu?.findItem(R.id.action_unfilter)?.isVisible = result.isFiltered
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.action_filter -> {
                    scope.launchIO {
                        viewModel.addFilter(result.tagName)
                    }
                }
                R.id.action_unfilter -> {
                    scope.launchIO {
                        viewModel.removeFilter(result.tagName)
                    }
                }
                R.id.action_copy_tag_name -> {
                    copyToClipboard(result.tagName, result.tagName, true, binding.root)
                }
            }
            mode?.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            floatingActionMode = null
        }
    }

    inner class FloatingImageTagActionModeCallback(
        private val tagVo: TagVo,
        private val chip: Chip,
    ) : ActionMode.Callback {

        private val parent = WeakReference(this@TagSettingsSheet)

        private var curDialog: AlertDialog? = null

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.image_tag_actions, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.action_copy -> {
                    copyToClipboard(
                        tagVo.tagValue,
                        tagVo.tagValue,
                        true,
                        chip,
                    )
                }

                R.id.action_local_search -> {
                    searchTagCallback?.invoke(tagVo)
                }

                R.id.action_delete -> {
                    scope.launchUI {
                        if (curDialog?.isShowing == true) {
                            curDialog?.dismiss()
                        }
                        curDialog = MaterialAlertDialogBuilder(context)
                            .setTitle("Do you want to delete this tag?")
                            .setMessage("Deleting this tag will remove it from all relations.")
                            .setPositiveButton(R.string.delete) { dialog, _ ->
                                scope.launchIO {
                                    parent.get()?.viewModel?.deleteTag(tagVo)
                                }
                            }
                            .setNegativeButton(R.string.cancel) { _, _ -> }
                            .show()
                    }
                }
            }
            mode?.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            parent.get()?.floatingActionMode = null
        }
    }

    private fun copyToClipboard(
        content: String,
        label: String?,
        useToast: Boolean,
        view: View,
    ): Snackbar? {
        if (content.isBlank()) return null

        val clipboard = activity.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content))

        label ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return null
        return if (useToast) {
            activity.toast(context.getString(R.string._copied_to_clipboard, label))
            null
        } else {
            view.snack(context.getString(R.string._copied_to_clipboard, label))
        }
    }
}
