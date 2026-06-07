package eu.kanade.tachiyomi.ui.reader.slide.engine

import android.graphics.PointF
import android.os.SystemClock
import android.view.Choreographer
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import timber.log.Timber

/**
 * 幻灯片动画引擎
 * 负责驱动图片的幻灯片播放效果
 *
 * @param imageView 要应用动画的图片视图
 * @param animationPath 动画路径配置
 */
class SlideAnimationEngine(
    private val imageView: SubsamplingScaleImageView?,
    var animationPath: SlideAnimationPath,
) {
    private var choreographer: Choreographer? = null
    private var frameCallback: Choreographer.FrameCallback? = null
    private var startTime: Long = 0L
    private var isRunning = false

    var elapsedTime: Long = 0L

    // 动画完成回调
    var onAnimationComplete: (() -> Unit)? = null

    // 每帧进度回调（用于同步时间轴等 UI）
    var onFrameUpdate: ((elapsedTimeMs: Long) -> Unit)? = null

    // 跟踪平移限制是否已放宽（用于避免重复设置）
    private var isPanLimitRelaxed: Boolean = false

    /**
     * 启动动画
     */
    fun start() {
        if (isRunning) {
            Timber.w("Animation is already running")
            return
        }

        if (imageView == null) {
            Timber.w("ImageView is null, cannot start animation")
            return
        }

        // 保存原始平移限制并放宽
        relaxPanLimit()

        isRunning = true
        startTime = SystemClock.elapsedRealtime()
        choreographer = Choreographer.getInstance()

        frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isRunning) {
                    return
                }

                elapsedTime = SystemClock.elapsedRealtime() - startTime

                if (elapsedTime >= animationPath.durationMs) {
                    // 动画结束
                    applyState(animationPath.getStateAtTime(animationPath.durationMs))
                    onFrameUpdate?.invoke(animationPath.durationMs)
                    onAnimationComplete?.invoke()
                    stop()
                } else {
                    // 应用当前帧状态
                    val state = animationPath.getStateAtTime(elapsedTime)
                    applyState(state)

                    // 通知进度更新
                    onFrameUpdate?.invoke(elapsedTime)

                    // 请求下一帧
                    choreographer?.postFrameCallback(this)
                }
            }
        }

        choreographer?.postFrameCallback(frameCallback!!)
        Timber.d("Slide animation started, duration: ${animationPath.durationMs}ms")
    }

    /**
     * 停止动画
     *
     * 注意：不再自动恢复 pan limit。pan limit 的生命周期由 ReaderActivity 的
     * 幻灯片模式（isTimelinePanelExpanded）统一管理，以避免动画结束后
     * 强制弹回填屏位置造成的视觉跳动。
     */
    fun stop() {
        isRunning = false
        frameCallback?.let {
            choreographer?.removeFrameCallback(it)
        }
        frameCallback = null
        choreographer = null

        Timber.d("Slide animation stopped")
    }

    /**
     * 暂停动画
     */
    fun pause() {
        if (isRunning) {
            frameCallback?.let {
                choreographer?.removeFrameCallback(it)
            }
        }
    }

    /**
     * 恢复动画
     */
    fun resume() {
        if (isRunning) {
            // 调整开始时间以实现无缝恢复（扣除暂停期间的时间）
            startTime = SystemClock.elapsedRealtime() - elapsedTime
            frameCallback?.let {
                choreographer?.postFrameCallback(it)
            }
        }
    }

    /**
     * 重置动画到初始状态
     *
     * 停止动画并将图片恢复到第一个关键帧位置。
     * pan limit 由外部（ReaderActivity）根据幻灯片模式统一管理。
     */
    fun reset() {
        stop()
        if (imageView != null) {
            applyState(animationPath.getStateAtTime(0))
        }
    }

    /**
     * 放宽平移限制 — 允许图片自由定位（幻灯片模式下调用）
     *
     * 使用 PAN_LIMIT_OUTSIDE（允许图片边缘超过屏幕边缘）
     * 这样可以确保在不同屏幕方向上都能应用相同的关键帧。
     * 由 ReaderActivity 根据幻灯片模式生命周期统一管理。
     */
    fun relaxPanLimit() {
        if (imageView == null || isPanLimitRelaxed) return

        imageView.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_OUTSIDE)
        isPanLimitRelaxed = true
        Timber.d("Pan limit relaxed to PAN_LIMIT_OUTSIDE")
    }

    /**
     * 恢复默认平移限制 — 退出幻灯片模式时调用
     *
     * 恢复为 PAN_LIMIT_INSIDE，图片自动回弹填满屏幕。
     * 由 ReaderActivity 在退出幻灯片模式时调用。
     */
    fun restorePanLimit() {
        if (imageView == null || !isPanLimitRelaxed) return

        imageView.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
        isPanLimitRelaxed = false
        Timber.d("Pan limit restored to PAN_LIMIT_INSIDE")
    }

    /**
     * 重新计算当前状态并应用
     */
    fun reCalculateStateAndApply() {
        animationPath.getStateAtTime(elapsedTime).apply {
            applyState(this)
        }
    }

    /**
     * 将视图状态应用到图片视图
     *
     * 使用 normalizedSourceCenter 直接还原为 sourceCenter 坐标，
     * 避免了 V2 版本中 translation → sourceCenter 转换在横竖屏切换时的误差。
     *
     * 归一化规则：
     * - normalizedScale: 相对于 fitScale 的倍数
     * - normalizedSourceCenterX/Y: 相对于 sWidth/sHeight 的比例（0.0~1.0）
     *
     * 注意：平移限制由动画级别管理（start/stop），此处不需要处理
     */
    fun applyState(state: ImageViewState) {
        if (imageView == null) return

        try {
            if (!imageView.isReady) {
                return
            }

            // 获取当前屏幕的 fitScale（适配屏幕的缩放值）
            val fitScale = imageView.minScale

            // 反归一化：计算实际的缩放值
            val actualScale = state.normalizedScale * fitScale

            // 反归一化：直接计算源图片中心坐标（横竖屏无关）
            val sourceCenterX = state.normalizedSourceCenterX * imageView.sWidth
            val sourceCenterY = state.normalizedSourceCenterY * imageView.sHeight

            val sourceCenter = PointF(sourceCenterX, sourceCenterY)

            // 应用缩放和中心
            // 平移限制已由动画级别管理（start/stop时设置 PAN_LIMIT_OUTSIDE）
            imageView.setScaleAndCenter(actualScale, sourceCenter)

            // 应用旋转
            imageView.rotation = state.rotation

            // 应用透明度
            imageView.alpha = state.alpha
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply image state: $state")
        }
    }

    /**
     * 获取当前动画进度（0-1）
     */
    fun getProgress(): Float {
        if (!isRunning) return 0f
        val elapsedTime = SystemClock.elapsedRealtime() - startTime
        return (elapsedTime.toFloat() / animationPath.durationMs).coerceIn(0f, 1f)
    }

    /**
     * 跳转到指定进度
     *
     * pan limit 由外部统一管理，此处直接应用状态不做 relax/restore。
     *
     * @param progress 进度值（0-1）
     */
    fun seekTo(progress: Float) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val targetTime = (animationPath.durationMs * clampedProgress).toLong()
        val state = animationPath.getStateAtTime(targetTime)

        applyState(state)

        // 如果正在运行，调整开始时间
        if (isRunning) {
            startTime = SystemClock.elapsedRealtime() - targetTime
        }
    }

    /**
     * 是否正在运行
     */
    fun isActive(): Boolean = isRunning
}
