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
            return ImageViewState.default()
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
            normalizedSourceCenterX = lerp(start.normalizedSourceCenterX, end.normalizedSourceCenterX, progress),
            normalizedSourceCenterY = lerp(start.normalizedSourceCenterY, end.normalizedSourceCenterY, progress),
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
) {
    companion object {
        fun default() =
            KeyFrame(0L, ImageViewState.default())
    }
}

/**
 * 扩展函数：创建放大缩小的关键帧路径
 *
 * @param durationMs 动画总时长
 * @param fromScale 起始缩放比例（相对于 fitScale）
 * @param toScale 目标缩放比例（相对于 fitScale）
 * @param centerXFrac 缩放中心的源图片X坐标（0-1，相对于 sWidth，0.5=图片中心）
 * @param centerYFrac 缩放中心的源图片Y坐标（0-1，相对于 sHeight，0.5=图片中心）
 */
fun createScaleAnimationPath(
    durationMs: Long,
    fromScale: Float = 1f,
    toScale: Float = 1.5f,
    centerXFrac: Float = 0.5f,
    centerYFrac: Float = 0.5f,
): SlideAnimationPath {
    val startState = ImageViewState(
        normalizedSourceCenterX = centerXFrac,
        normalizedSourceCenterY = centerYFrac,
        normalizedScale = fromScale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val endState = ImageViewState(
        normalizedSourceCenterX = centerXFrac,
        normalizedSourceCenterY = centerYFrac,
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
            KeyFrame(durationMs, endState),
        ),
    )
}

/**
 * 扩展函数：创建摄像机移动的关键帧路径
 *
 * @param durationMs 动画总时长
 * @param startXFrac 起始源图片X坐标（0-1，相对于 sWidth）
 * @param startYFrac 起始源图片Y坐标（0-1，相对于 sHeight）
 * @param endXFrac 目标源图片X坐标（0-1，相对于 sWidth）
 * @param endYFrac 目标源图片Y坐标（0-1，相对于 sHeight）
 * @param scale 缩放比例（相对于 fitScale）
 */
fun createPanAnimationPath(
    durationMs: Long,
    startXFrac: Float = 0.5f,
    startYFrac: Float = 0.5f,
    endXFrac: Float = 0.6f,
    endYFrac: Float = 0.6f,
    scale: Float = 1.2f,
): SlideAnimationPath {
    val startState = ImageViewState(
        normalizedSourceCenterX = startXFrac,
        normalizedSourceCenterY = startYFrac,
        normalizedScale = scale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val endState = ImageViewState(
        normalizedSourceCenterX = endXFrac,
        normalizedSourceCenterY = endYFrac,
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
 * @param startXFrac 起始源图片X坐标（0-1）
 * @param startYFrac 起始源图片Y坐标（0-1）
 * @param endXFrac 目标源图片X坐标（0-1）
 * @param endYFrac 目标源图片Y坐标（0-1）
 */
fun createCombinedAnimationPath(
    durationMs: Long,
    fromScale: Float = 1f,
    toScale: Float = 1.3f,
    startXFrac: Float = 0.45f,
    startYFrac: Float = 0.47f,
    endXFrac: Float = 0.55f,
    endYFrac: Float = 0.53f,
): SlideAnimationPath {
    val startState = ImageViewState(
        normalizedSourceCenterX = startXFrac,
        normalizedSourceCenterY = startYFrac,
        normalizedScale = fromScale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val middleState = ImageViewState(
        normalizedSourceCenterX = 0.5f,
        normalizedSourceCenterY = 0.5f,
        normalizedScale = toScale,
        rotation = 0f,
        pivotX = 0.5f,
        pivotY = 0.5f,
        alpha = 1f,
    )

    val endState = ImageViewState(
        normalizedSourceCenterX = endXFrac,
        normalizedSourceCenterY = endYFrac,
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
