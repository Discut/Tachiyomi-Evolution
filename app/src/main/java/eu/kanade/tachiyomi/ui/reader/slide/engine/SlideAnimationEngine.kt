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
            // 调整开始时间以实现无缝恢复
            startTime = SystemClock.elapsedRealtime() - (SystemClock.elapsedRealtime() - startTime)
            frameCallback?.let {
                choreographer?.postFrameCallback(it)
            }
        }
    }

    /**
     * 重置动画到初始状态
     */
    fun reset() {
        stop()
        if (imageView != null) {
            applyState(animationPath.getStateAtTime(0))
        }
    }

    /**
     * 将视图状态应用到图片视图
     */
    fun applyState(state: ImageViewState) {
        if (imageView == null) return

        try {
            if (!imageView.isReady) {
                return
            }

            // ✅ 只保留必要的计算：视图中心
            val viewCenterX = imageView.sWidth / 2f
            val viewCenterY = imageView.sHeight / 2f

            // ✅ 根据公式计算源图片中心点
            val sourceCenterX = viewCenterX - state.translationX / state.scaleX
            val sourceCenterY = viewCenterY - state.translationY / state.scaleY

            val sourceCenter = PointF(sourceCenterX, sourceCenterY)

            // ✅ 应用缩放和中心
            val scale = state.scaleX
            imageView.setScaleAndCenter(scale, sourceCenter)

            // ✅ 应用旋转
            imageView.rotation = state.rotation

            // ✅ 应用透明度
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
