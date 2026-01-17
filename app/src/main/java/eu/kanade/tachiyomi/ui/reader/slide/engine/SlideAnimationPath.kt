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
            translationX = lerp(start.translationX, end.translationX, progress),
            translationY = lerp(start.translationY, end.translationY, progress),
            scaleX = lerp(start.scaleX, end.scaleX, progress),
            scaleY = lerp(start.scaleY, end.scaleY, progress),
            rotation = lerp(start.rotation, end.rotation, progress),
            pivotX = lerp(start.pivotX, end.pivotX, progress),
            pivotY = lerp(start.pivotY, end.pivotY, progress),
            alpha = lerp(start.alpha, end.alpha, progress),
        )
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float {
        return start + (end - start) * progress
    }
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
 * 扩展函数：创建默认视图状态
 */
fun ImageViewState.Companion.default() = ImageViewState(
    translationX = 0f,
    translationY = 0f,
    scaleX = 1f,
    scaleY = 1f,
    rotation = 0f,
    pivotX = 0.5f,
    pivotY = 0.5f,
    alpha = 1f,
)

/**
 * 扩展函数：创建放大缩小的关键帧路径
 *
 * @param durationMs 动画总时长
 * @param fromScale 起始缩放比例
 * @param toScale 目标缩放比例
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
        translationX = 0f,
        translationY = 0f,
        scaleX = fromScale,
        scaleY = fromScale,
        rotation = 0f,
        pivotX = centerX,
        pivotY = centerY,
        alpha = 1f,
    )

    val endState = ImageViewState(
        translationX = 0f,
        translationY = 0f,
        scaleX = toScale,
        scaleY = toScale,
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
 * @param startX 起始X偏移（像素）
 * @param startY 起始Y偏移（像素）
 * @param endX 目标X偏移（像素）
 * @param endY 目标Y偏移（像素）
 * @param scale 缩放比例
 */
fun createPanAnimationPath(
    durationMs: Long,
    startX: Float = 0f,
    startY: Float = 0f,
    endX: Float = 100f,
    endY: Float = 100f,
    scale: Float = 1.2f,
): SlideAnimationPath {
    val startState = ImageViewState(
        translationX = startX,
        translationY = startY,
        scaleX = scale,
        scaleY = scale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val endState = ImageViewState(
        translationX = endX,
        translationY = endY,
        scaleX = scale,
        scaleY = scale,
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
 * @param fromScale 起始缩放
 * @param toScale 目标缩放
 * @param startX 起始X偏移
 * @param startY 起始Y偏移
 * @param endX 目标X偏移
 * @param endY 目标Y偏移
 */
fun createCombinedAnimationPath(
    durationMs: Long,
    fromScale: Float = 1f,
    toScale: Float = 1.3f,
    startX: Float = -50f,
    startY: Float = -30f,
    endX: Float = 50f,
    endY: Float = 30f,
): SlideAnimationPath {
    val startState = ImageViewState(
        translationX = startX,
        translationY = startY,
        scaleX = fromScale,
        scaleY = fromScale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val middleState = ImageViewState(
        translationX = 0f,
        translationY = 0f,
        scaleX = toScale,
        scaleY = toScale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val endState = ImageViewState(
        translationX = endX,
        translationY = endY,
        scaleX = toScale,
        scaleY = toScale,
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
