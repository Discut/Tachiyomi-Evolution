package eu.kanade.tachiyomi.ui.reader.slide.engine

/**
 * 动画路径配置
 * 定义单个图片的幻灯片播放路径
 *
 * @param durationMs 动画总时长（毫秒）
 * @param keyFrames 关键帧列表，按时间顺序排列
 */
data class SlideAnimationPath(
    val durationMs: Long,
    val keyFrames: List<KeyFrame>,
) {

    companion object {
        fun empty() = SlideAnimationPath(5000L, emptyList())
    }

    /**
     * 获取指定时间点的视图状态
     *
     * @param elapsedTimeMs 已经播放的时间（毫秒）
     * @return 该时刻的视图状态
     */
    fun getStateAtTime(elapsedTimeMs: Long): ImageViewState {
        if (keyFrames.isEmpty()) {
            return ImageViewState.Companion.default()
        }

        // 如果超出总时长，返回最后一个关键帧状态
        if (elapsedTimeMs >= durationMs) {
            return keyFrames.last().state
        }

        // 找到当前时间前后的关键帧
        var startFrame = keyFrames[0]
        var endFrame = keyFrames[0]

        for (i in keyFrames.indices) {
            if (keyFrames[i].timeMs <= elapsedTimeMs) {
                startFrame = keyFrames[i]
                if (i < keyFrames.size - 1) {
                    endFrame = keyFrames[i + 1]
                }
            }
        }

        // 计算插值
        val progress = if (endFrame.timeMs > startFrame.timeMs) {
            (elapsedTimeMs - startFrame.timeMs).toFloat() / (endFrame.timeMs - startFrame.timeMs)
        } else {
            1f
        }

        return interpolate(startFrame.state, endFrame.state, progress)
    }

    /**
     * 在两个状态之间进行线性插值
     */
    private fun interpolate(
        start: ImageViewState,
        end: ImageViewState,
        progress: Float,
    ): ImageViewState {
        return ImageViewState(
            normalizedTranslationX = lerp(start.normalizedTranslationX, end.normalizedTranslationX, progress),
            normalizedTranslationY = lerp(start.normalizedTranslationY, end.normalizedTranslationY, progress),
            normalizedScale = lerp(start.normalizedScale, end.normalizedScale, progress),
            rotation = lerp(start.rotation, end.rotation, progress),
            pivotX = lerp(start.pivotX, end.pivotX, progress),
            pivotY = lerp(start.pivotY, end.pivotY, progress),
            alpha = lerp(start.alpha, end.alpha, progress),
            // 保留第一个关键帧的元数据
            imageWidth = start.imageWidth,
            imageHeight = start.imageHeight,
        )
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float {
        return start + (end - start) * progress
    }
}

fun SlideAnimationPath?.isEmpty(): Boolean {
    return this?.keyFrames?.isEmpty() ?: true
}

/**
 * 关键帧
 * 定义特定时间点的视图状态
 *
 * @param timeMs 相对起始时间的偏移量（毫秒）
 * @param state 该时间点的视图状态
 */
data class KeyFrame(
    val timeMs: Long,
    val state: ImageViewState,
)

/**
 * 扩展函数：创建放大缩小的关键帧路径
 *
 * @param durationMs 动画总时长
 * @param fromScale 起始缩放比例（相对于 fitScale）
 * @param toScale 目标缩放比例（相对于 fitScale）
 * @param centerX 缩放中心X坐标（0-1）
 * @param centerY 缩放中心Y坐标（0-1）
 */
fun createScaleAnimationPath(
    durationMs: Long,
    fromScale: Float = 1f,
    toScale: Float = 1.5f,
    centerX: Float = 0.5f,
    centerY: Float = 0.5f,
): SlideAnimationPath {
    val startState = ImageViewState(
        normalizedTranslationX = 0f,
        normalizedTranslationY = 0f,
        normalizedScale = fromScale,
        rotation = 0f,
        pivotX = centerX,
        pivotY = centerY,
        alpha = 1f,
    )

    val endState = ImageViewState(
        normalizedTranslationX = 0f,
        normalizedTranslationY = 0f,
        normalizedScale = toScale,
        rotation = 0f,
        pivotX = centerX,
        pivotY = centerY,
        alpha = 1f,
    )

    return SlideAnimationPath(
        durationMs = durationMs,
        keyFrames = listOf(
            KeyFrame(0L, startState),
            KeyFrame(durationMs, endState),
        ),
    )
}

/**
 * 扩展函数：创建摄像机移动的关键帧路径
 *
 * @param durationMs 动画总时长
 * @param startX 起始X偏移（相对于图片宽度，0-1）
 * @param startY 起始Y偏移（相对于图片高度，0-1）
 * @param endX 目标X偏移（相对于图片宽度，0-1）
 * @param endY 目标Y偏移（相对于图片高度，0-1）
 * @param scale 缩放比例（相对于 fitScale）
 */
fun createPanAnimationPath(
    durationMs: Long,
    startX: Float = 0f,
    startY: Float = 0f,
    endX: Float = 0.1f,
    endY: Float = 0.1f,
    scale: Float = 1.2f,
): SlideAnimationPath {
    val startState = ImageViewState(
        normalizedTranslationX = startX,
        normalizedTranslationY = startY,
        normalizedScale = scale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val endState = ImageViewState(
        normalizedTranslationX = endX,
        normalizedTranslationY = endY,
        normalizedScale = scale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    return SlideAnimationPath(
        durationMs = durationMs,
        keyFrames = listOf(
            KeyFrame(0L, startState),
            KeyFrame(durationMs, endState),
        ),
    )
}

/**
 * 扩展函数：创建组合动画路径（缩放+移动）
 *
 * @param durationMs 动画总时长
 * @param fromScale 起始缩放（相对于 fitScale）
 * @param toScale 目标缩放（相对于 fitScale）
 * @param startX 起始X偏移（相对于图片宽度，0-1）
 * @param startY 起始Y偏移（相对于图片高度，0-1）
 * @param endX 目标X偏移（相对于图片宽度，0-1）
 * @param endY 目标Y偏移（相对于图片高度，0-1）
 */
fun createCombinedAnimationPath(
    durationMs: Long,
    fromScale: Float = 1f,
    toScale: Float = 1.3f,
    startX: Float = -0.05f,
    startY: Float = -0.03f,
    endX: Float = 0.05f,
    endY: Float = 0.03f,
): SlideAnimationPath {
    val startState = ImageViewState(
        normalizedTranslationX = startX,
        normalizedTranslationY = startY,
        normalizedScale = fromScale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val middleState = ImageViewState(
        normalizedTranslationX = 0f,
        normalizedTranslationY = 0f,
        normalizedScale = toScale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val endState = ImageViewState(
        normalizedTranslationX = endX,
        normalizedTranslationY = endY,
        normalizedScale = toScale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    return SlideAnimationPath(
        durationMs = durationMs,
        keyFrames = listOf(
            KeyFrame(0L, startState),
            KeyFrame(durationMs / 2, middleState),
            KeyFrame(durationMs, endState),
        ),
    )
}
