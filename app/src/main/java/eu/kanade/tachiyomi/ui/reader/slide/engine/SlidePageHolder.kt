package eu.kanade.tachiyomi.ui.reader.slide.engine

import android.annotation.SuppressLint
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPageHolder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer

/**
 * 支持幻灯片动画的PageHolder
 * 继承自PagerPageHolder，添加了动画播放功能
 */
@SuppressLint("ViewConstructor")
class SlidePageHolder(
    viewer: PagerViewer,
    page: ReaderPage,
    private var extraPage: ReaderPage? = null,
) : PagerPageHolder(viewer, page, extraPage) {

    /**
     * 动画引擎实例
     */
    var animationEngine: SlideAnimationEngine? = null

    /**
     * 动画路径配置
     */
    var animationPath: SlideAnimationPath?
        get() {
            return animationEngine?.animationPath
        }
        set(value) {
            if (value == null) {
                return
            }
            animationEngine?.animationPath = value
        }

    /**
     * 是否启用动画
     */
    var isAnimationEnabled: Boolean = true

    /**
     * 动画完成回调
     */
    var onAnimationComplete: (() -> Unit)? = null

    var onHolderEvent: OnHolderEvent? = null

    /**
     * 获取当前的图片视图，用于读取变换状态
     */
    fun getImageView(): SubsamplingScaleImageView? {
        return pageView as? SubsamplingScaleImageView
    }

    override fun onImage() {
        super.onImage()
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
/*        if (isAnimationEnabled) {
            startAnimation()
        } else {
            stopAnimation()
        }*/
        onHolderEvent?.onLoaded(this)
    }

    /**
     * 启动幻灯片动画
     */
    fun startAnimation() {
        val path = animationPath ?: return
        val imageView = pageView as? SubsamplingScaleImageView ?: return

        // 确保图片已加载
        if (!imageView.isReady) {
            // 图片未就绪，延迟启动
            return
        }

        // 停止之前的动画
        animationEngine?.stop()

        // 创建并启动新动画
        animationEngine = SlideAnimationEngine(imageView, path).apply {
            onAnimationComplete = {
                this@SlidePageHolder.onAnimationComplete?.invoke()
            }
        }
    }

    /**
     * 停止幻灯片动画
     */
    fun stopAnimation() {
        animationEngine?.stop()
        animationEngine = null
    }

    /**
     * 暂停动画
     */
    fun pauseAnimation() {
        animationEngine?.pause()
    }

    /**
     * 恢复动画
     */
    fun resumeAnimation() {
        animationEngine?.resume()
    }

    /**
     * 重置动画到初始状态
     */
    fun resetAnimation() {
        animationEngine?.reset()
    }

    /**
     * 获取当前动画进度（0-1）
     */
    fun getAnimationProgress(): Float {
        return animationEngine?.getProgress() ?: 0f
    }

    /**
     * 跳转到指定动画进度
     *
     * @param progress 进度值（0-1）
     */
    fun seekAnimationTo(progress: Float) {
        animationEngine?.seekTo(progress)
    }

    /**
     * 设置动画路径并启动动画
     *
     * @param path 动画路径配置
     * @param autoStart 是否自动启动动画
     */
    fun setAnimationPath(path: SlideAnimationPath, autoStart: Boolean = true) {
        this.animationPath = path
        if (autoStart && isAnimationEnabled) {
            startAnimation()
        }
    }

    /**
     * 使用预定义的动画类型
     *
     * @param type 动画类型
     * @param durationMs 动画时长
     */
    @Deprecated("")
    fun setAnimationByType(type: AnimationType, durationMs: Long = 3000L) {
        val path = when (type) {
            AnimationType.SCALE_CENTER -> createScaleAnimationPath(
                durationMs = durationMs,
                fromScale = 1.0f,
                toScale = 1.5f,
                centerX = 0.5f,
                centerY = 0.5f,
            )
            AnimationType.PAN_HORIZONTAL -> createPanAnimationPath(
                durationMs = durationMs,
                startX = -100f,
                startY = 0f,
                endX = 100f,
                endY = 0f,
                scale = 1.2f,
            )
            AnimationType.PAN_VERTICAL -> createPanAnimationPath(
                durationMs = durationMs,
                startX = 0f,
                startY = -80f,
                endX = 0f,
                endY = 80f,
                scale = 1.2f,
            )
            AnimationType.DIAGONAL -> createCombinedAnimationPath(
                durationMs = durationMs,
                fromScale = 1.0f,
                toScale = 1.3f,
                startX = -80f,
                startY = -50f,
                endX = 80f,
                endY = 50f,
            )
        }
        setAnimationPath(path, isAnimationEnabled)
    }

    /**
     * 覆盖父类的onDetachedFromWindow，确保动画被清理
     */
    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    /**
     * 动画类型枚举
     */
    enum class AnimationType {
        /** 中心放大缩小 */
        SCALE_CENTER,

        /** 水平移动 */
        PAN_HORIZONTAL,

        /** 垂直移动 */
        PAN_VERTICAL,

        /** 对角线移动+缩放 */
        DIAGONAL,
    }
}
