package eu.kanade.tachiyomi.ui.reader.slide.engine

/**
 * 图片视图状态
 *
 * 使用归一化值存储，确保在不同屏幕尺寸和方向上保持一致的视觉效果。
 *
 * @param normalizedTranslationX 归一化的水平平移（相对于图片宽度，0-1）
 * @param normalizedTranslationY 归一化的垂直平移（相对于图片高度，0-1）
 * @param normalizedScale 归一化的缩放比例（相对于 fitScale 的倍数）
 * @param rotation 旋转角度（度）
 * @param pivotX 缩放/旋转中心X坐标（0-1，相对于视图）
 * @param pivotY 缩放/旋转中心Y坐标（0-1，相对于视图）
 * @param alpha 透明度（0-1）
 * @param imageWidth 创建关键帧时的图片原始宽度（像素）
 * @param imageHeight 创建关键帧时的图片原始高度（像素）
 */
data class ImageViewState(
    // 归一化的平移（相对于图片尺寸，0-1）
    val normalizedTranslationX: Float = 0f,

    val normalizedTranslationY: Float = 0f,

    // 归一化的缩放（相对于 fitScale 的倍数）
    // 1.0 = 适配屏幕大小
    // 2.0 = 放大到适配屏幕的2倍
    val normalizedScale: Float = 1f,

    // 以下属性保持不变
    val rotation: Float = 0f,
    val pivotX: Float = 0.5f,
    val pivotY: Float = 0.5f,
    val alpha: Float = 1f,

    // 元数据：创建时的图片尺寸
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
) {
    companion object {
        /**
         * 创建默认视图状态
         */
        fun default() = ImageViewState(
            normalizedTranslationX = 0f,
            normalizedTranslationY = 0f,
            normalizedScale = 1f,
            rotation = 0f,
            pivotX = 0.5f,
            pivotY = 0.5f,
            alpha = 1f,
            imageWidth = 0,
            imageHeight = 0,
        )

        /**
         * 从旧格式创建（用于迁移）
         */
        fun fromLegacy(
            translationX: Float,
            translationY: Float,
            scaleX: Float,
            scaleY: Float,
            rotation: Float,
            pivotX: Float,
            pivotY: Float,
            alpha: Float,
        ): ImageViewState {
            return ImageViewState(
                normalizedTranslationX = translationX,
                normalizedTranslationY = translationY,
                normalizedScale = scaleX,
                rotation = rotation,
                pivotX = pivotX,
                pivotY = pivotY,
                alpha = alpha,
            )
        }
    }

    /**
     * 向后兼容：scaleX 属性
     * @deprecated 使用 normalizedScale 代替
     */
    val scaleX: Float
        get() = normalizedScale

    /**
     * 向后兼容：scaleY 属性
     * @deprecated 使用 normalizedScale 代替
     */
    val scaleY: Float
        get() = normalizedScale
}

/**
 * 从当前 ImageView 状态创建归一化的 ImageViewState
 *
 * @param currentScale 当前实际缩放值
 * @param fitScale 适配屏幕的缩放值
 * @param translationX 当前平移X（像素）
 * @param translationY 当前平移Y（像素）
 * @param imageWidth 图片原始宽度
 * @param imageHeight 图片原始高度
 * @param rotation 旋转角度
 * @param pivotX 缩放中心X
 * @param pivotY 缩放中心Y
 * @param alpha 透明度
 */
fun createNormalizedImageViewState(
    currentScale: Float,
    fitScale: Float,
    translationX: Float,
    translationY: Float,
    imageWidth: Int,
    imageHeight: Int,
    rotation: Float = 0f,
    pivotX: Float = 0.5f,
    pivotY: Float = 0.5f,
    alpha: Float = 1f,
): ImageViewState {
    return ImageViewState(
        normalizedScale = if (fitScale > 0) currentScale / fitScale else 1f,
        normalizedTranslationX = if (imageWidth > 0) translationX / imageWidth else 0f,
        normalizedTranslationY = if (imageHeight > 0) translationY / imageHeight else 0f,
        rotation = rotation,
        pivotX = pivotX,
        pivotY = pivotY,
        alpha = alpha,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
    )
}
