package eu.kanade.tachiyomi.ui.reader.slide.sheet

import android.app.Activity
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.databinding.KeyframeEditSheetBinding
import eu.kanade.tachiyomi.ui.reader.slide.engine.ImageViewState
import eu.kanade.tachiyomi.ui.reader.slide.engine.SlideAnimationPath

/**
 * 关键帧属性编辑 BottomSheet
 *
 * 提供 Position X/Y、Scale、Rotation、Alpha 五个属性滑块，
 * 以及一个总时长滑块，支持实时预览和导航到其他关键帧。
 */
class KeyframeEditSheet(
    private val activity: Activity,
    private val initialKeyFrameTimeMs: Long,
    initialImageViewState: ImageViewState,
    private val animationPath: SlideAnimationPath,
    private val onPropertyChanged: (Long, ImageViewState) -> Unit,
    private val onDeleteRequested: (Long) -> Unit,
    private val onDuplicateRequested: (Long) -> Unit,
    private val onDurationChanged: (Long) -> Unit,
    private val onCatchRequested: (Long) -> Unit,
) : BottomSheetDialog(activity) {

    companion object {
        /** 默认值，用于重置功能 */
        private const val DEFAULT_POS_X = 0.5f
        private const val DEFAULT_POS_Y = 0.5f
        private const val DEFAULT_SCALE = 1.0f
        private const val DEFAULT_ROTATION = 0.0f
        private const val DEFAULT_ALPHA = 1.0f
        private const val DEFAULT_DURATION_MS = 5000L
    }

    private val binding: KeyframeEditSheetBinding by lazy {
        KeyframeEditSheetBinding.inflate(activity.layoutInflater)
    }

    private var currentTimeMs: Long = initialKeyFrameTimeMs
    private var currentState: ImageViewState = initialImageViewState

    /** 防止滑块回调触发无限更新循环 */
    private var isUpdatingFromCallback: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initSliders()
        initButtons()
        initResetButtons()

        // 设置初始值
        updateTimeLabel()
        updateSliderValues(currentState)
    }

    private fun initSliders() {
        // Position X
        binding.kfSliderPosX.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || isUpdatingFromCallback) return@addOnChangeListener
            val newState = currentState.copy(normalizedSourceCenterX = value)
            applyPropertyChange(newState)
            binding.kfValuePosX.text = formatFloat(value, 2)
        }

        // Position Y
        binding.kfSliderPosY.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || isUpdatingFromCallback) return@addOnChangeListener
            val newState = currentState.copy(normalizedSourceCenterY = value)
            applyPropertyChange(newState)
            binding.kfValuePosY.text = formatFloat(value, 2)
        }

        // Scale
        binding.kfSliderScale.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || isUpdatingFromCallback) return@addOnChangeListener
            val newState = currentState.copy(normalizedScale = value)
            applyPropertyChange(newState)
            binding.kfValueScale.text = "${formatFloat(value, 1)}x"
        }

        // Rotation
        binding.kfSliderRotation.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || isUpdatingFromCallback) return@addOnChangeListener
            val newState = currentState.copy(rotation = value)
            applyPropertyChange(newState)
            binding.kfValueRotation.text = "${value.toInt()}°"
        }

        // Alpha
        binding.kfSliderAlpha.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || isUpdatingFromCallback) return@addOnChangeListener
            val newState = currentState.copy(alpha = value)
            applyPropertyChange(newState)
            binding.kfValueAlpha.text = "${(value * 100).toInt()}%"
        }

        // Total duration
        binding.kfSliderDuration.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || isUpdatingFromCallback) return@addOnChangeListener
            onDurationChanged(value.toLong())
            binding.kfValueDuration.text = formatTime(value.toLong())
        }
    }

    private fun initButtons() {
        binding.kfBtnDelete.setOnClickListener {
            onDeleteRequested(currentTimeMs)
            dismiss()
        }

        binding.kfBtnCopy.setOnClickListener {
            onDuplicateRequested(currentTimeMs)
            dismiss()
        }

        binding.kfBtnCatch.setOnClickListener {
            onCatchRequested(currentTimeMs)
        }
    }

    private fun initResetButtons() {
        // 重置全部属性
        binding.kfBtnResetAll.setOnClickListener {
            resetAllProperties()
        }

        // 单独重置 Position X
        binding.kfBtnResetPosX.setOnClickListener {
            resetSingleProperty { copy(normalizedSourceCenterX = DEFAULT_POS_X) }
            updateSingleSliderDisplay(
                binding.kfSliderPosX,
                binding.kfValuePosX,
                DEFAULT_POS_X,
                formatFloat(DEFAULT_POS_X, 2),
            )
        }

        // 单独重置 Position Y
        binding.kfBtnResetPosY.setOnClickListener {
            resetSingleProperty { copy(normalizedSourceCenterY = DEFAULT_POS_Y) }
            updateSingleSliderDisplay(
                binding.kfSliderPosY,
                binding.kfValuePosY,
                DEFAULT_POS_Y,
                formatFloat(DEFAULT_POS_Y, 2),
            )
        }

        // 单独重置 Scale
        binding.kfBtnResetScale.setOnClickListener {
            resetSingleProperty { copy(normalizedScale = DEFAULT_SCALE) }
            updateSingleSliderDisplay(
                binding.kfSliderScale,
                binding.kfValueScale,
                DEFAULT_SCALE,
                "${formatFloat(DEFAULT_SCALE, 1)}x",
            )
        }

        // 单独重置 Rotation
        binding.kfBtnResetRotation.setOnClickListener {
            resetSingleProperty { copy(rotation = DEFAULT_ROTATION) }
            updateSingleSliderDisplay(
                binding.kfSliderRotation,
                binding.kfValueRotation,
                DEFAULT_ROTATION,
                "${DEFAULT_ROTATION.toInt()}°",
            )
        }

        // 单独重置 Alpha
        binding.kfBtnResetAlpha.setOnClickListener {
            resetSingleProperty { copy(alpha = DEFAULT_ALPHA) }
            updateSingleSliderDisplay(
                binding.kfSliderAlpha,
                binding.kfValueAlpha,
                DEFAULT_ALPHA,
                "${(DEFAULT_ALPHA * 100).toInt()}%",
            )
        }

        // 单独重置 Duration
        binding.kfBtnResetDuration.setOnClickListener {
            val defaultMs = DEFAULT_DURATION_MS.toFloat()
            binding.kfSliderDuration.snapValue(defaultMs)
            binding.kfValueDuration.text = formatTime(DEFAULT_DURATION_MS)
            onDurationChanged(DEFAULT_DURATION_MS)
        }
    }

    /** 重置单个属性并触发实时预览 */
    private fun resetSingleProperty(stateTransform: ImageViewState.() -> ImageViewState) {
        val newState = currentState.stateTransform()
        applyPropertyChange(newState)
    }

    /** 更新单个滑块的显示值和文本 */
    private fun updateSingleSliderDisplay(
        slider: com.google.android.material.slider.Slider,
        valueView: android.widget.TextView,
        newValue: Float,
        displayText: String,
    ) {
        isUpdatingFromCallback = true
        slider.snapValue(newValue)
        valueView.text = displayText
        isUpdatingFromCallback = false
    }

    /** 重置全部属性到默认值 */
    private fun resetAllProperties() {
        val newState = ImageViewState(
            normalizedSourceCenterX = DEFAULT_POS_X,
            normalizedSourceCenterY = DEFAULT_POS_Y,
            normalizedScale = DEFAULT_SCALE,
            rotation = DEFAULT_ROTATION,
            pivotX = 0.5f,
            pivotY = 0.5f,
            alpha = DEFAULT_ALPHA,
            imageWidth = currentState.imageWidth,
            imageHeight = currentState.imageHeight,
        )
        applyPropertyChange(newState)
        updateAllSliderDisplays(newState)
    }

    /** 更新全部滑块的显示 */
    private fun updateAllSliderDisplays(state: ImageViewState) {
        isUpdatingFromCallback = true
        binding.kfSliderPosX.snapValue(state.normalizedSourceCenterX)
        binding.kfSliderPosY.snapValue(state.normalizedSourceCenterY)
        binding.kfSliderScale.snapValue(state.normalizedScale)
        binding.kfSliderRotation.snapValue(state.rotation)
        binding.kfSliderAlpha.snapValue(state.alpha)

        binding.kfValuePosX.text = formatFloat(binding.kfSliderPosX.value, 2)
        binding.kfValuePosY.text = formatFloat(binding.kfSliderPosY.value, 2)
        binding.kfValueScale.text = "${formatFloat(binding.kfSliderScale.value, 1)}x"
        binding.kfValueRotation.text = "${binding.kfSliderRotation.value.toInt()}°"
        binding.kfValueAlpha.text = "${(binding.kfSliderAlpha.value * 100).toInt()}%"
        isUpdatingFromCallback = false
    }

    private fun applyPropertyChange(newState: ImageViewState) {
        currentState = newState
        onPropertyChanged(currentTimeMs, newState)
    }

    /**
     * 用于在切换关键帧时从外部更新面板状态
     */
    fun updateKeyFrameTime(newTimeMs: Long, newState: ImageViewState) {
        isUpdatingFromCallback = true
        currentTimeMs = newTimeMs
        currentState = newState
        updateTimeLabel()
        updateSliderValues(newState)
        isUpdatingFromCallback = false
    }

    private fun updateTimeLabel() {
        binding.kfTitle.text = "关键帧编辑: ${(currentTimeMs.toFloat() / 1000f).format(1)}s"
    }

    private fun updateSliderValues(state: ImageViewState) {
        binding.kfSliderPosX.snapValue(state.normalizedSourceCenterX)
        binding.kfSliderPosY.snapValue(state.normalizedSourceCenterY)
        binding.kfSliderScale.snapValue(state.normalizedScale)
        binding.kfSliderRotation.snapValue(state.rotation)
        binding.kfSliderAlpha.snapValue(state.alpha)

        binding.kfValuePosX.text = formatFloat(binding.kfSliderPosX.value, 2)
        binding.kfValuePosY.text = formatFloat(binding.kfSliderPosY.value, 2)
        binding.kfValueScale.text = "${formatFloat(binding.kfSliderScale.value, 1)}x"
        binding.kfValueRotation.text = "${binding.kfSliderRotation.value.toInt()}°"
        binding.kfValueAlpha.text = "${(binding.kfSliderAlpha.value * 100).toInt()}%"

        // Duration
        val durationMs = animationPath.durationMs.toFloat().coerceAtLeast(binding.kfSliderDuration.valueFrom)
            .coerceAtMost(binding.kfSliderDuration.valueTo)
        binding.kfSliderDuration.snapValue(durationMs)
        binding.kfValueDuration.text = formatTime(binding.kfSliderDuration.value.toLong())
    }

    /**
     * 将值对齐到 Slider 的 stepSize
     */
    private fun com.google.android.material.slider.Slider.snapValue(value: Float) {
        val step = stepSize
        val snapped = if (step > 0f) {
            (kotlin.math.round((value - valueFrom) / step) * step + valueFrom)
                .coerceIn(valueFrom, valueTo)
        } else {
            value.coerceIn(valueFrom, valueTo)
        }
        this.value = snapped
    }

    private fun formatFloat(value: Float, digits: Int): String {
        return "%.${digits}f".format(value)
    }

    private fun formatTime(ms: Long): String {
        return "${(ms / 1000f).format(1)}s"
    }

    private fun Float.format(digits: Int): String {
        return "%.${digits}f".format(this)
    }
}
