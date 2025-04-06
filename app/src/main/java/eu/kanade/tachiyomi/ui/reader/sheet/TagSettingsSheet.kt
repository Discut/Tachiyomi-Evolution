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
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.core.view.allViews
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderTagSettingsSheetBinding
import eu.kanade.tachiyomi.model.ImageBO
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
    private val imageBO: ImageBO,
    private val searchTagCallback: ((TagVo) -> Unit)? = null,
) :
    BottomSheetDialog(activity) {

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    val viewModel by lazy { TagSettingsViewModel(activity, imageBO) }

    val binding: ReaderTagSettingsSheetBinding by lazy { createBinding(activity.layoutInflater) }

    private var floatingActionMode: ActionMode? = null

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
                    TagNewDialog().show(activity) { scope.launchIO { viewModel.createNewTag(it) } }
                }
            }

            !tagVo.isSelected -> {
                scope.launchIO { viewModel.relatedImageAndTag(imageBO, tagVo) }
            }

            tagVo.isSelected -> {
                scope.launchIO { viewModel.unrelatedImageAndTag(imageBO, tagVo) }
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

    private fun finishFloatingActionMode() {
        floatingActionMode ?: return
        floatingActionMode?.finish()
        floatingActionMode = null
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
