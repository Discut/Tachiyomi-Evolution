package eu.kanade.tachiyomi.ui.reader.slide.engine

/**
 * 图片视图状态
 *
 * 使用归一化值存储，确保在不同屏幕尺寸和方向上保持一致的视觉效果。
 *
 * 关键设计：使用 normalizedSourceCenterX/Y（相对于图片原始像素尺寸）替代 normalizedTranslationX/Y。
 * SubsamplingScaleImageView 的原生坐标 sourceCenter 本身就是横竖屏无关的，
 * 直接存储归一化的 sourceCenter 可以避免 viewport-relative translation 在横竖屏切换时的变换误差。
 *
 * @param normalizedSourceCenterX 归一化的源图片中心X坐标（0.0~1.0，相对于 sWidth，0.5=图片中心）
 * @param normalizedSourceCenterY 归一化的源图片中心Y坐标（0.0~1.0，相对于 sHeight，0.5=图片中心）
 * @param normalizedScale 归一化的缩放比例（相对于 fitScale 的倍数）
 * @param rotation 旋转角度（度）
 * @param pivotX 缩放/旋转中心X坐标（0-1，相对于视图）
 * @param pivotY 缩放/旋转中心Y坐标（0-1，相对于视图）
 * @param alpha 透明度（0-1）
 * @param imageWidth 创建关键帧时的图片原始宽度（像素），仅元数据
 * @param imageHeight 创建关键帧时的图片原始高度（像素），仅元数据
 */
data class ImageViewState(
    // 归一化的源图片中心坐标（相对于图片原始尺寸，0.0~1.0）
    // 0.5 = 图片中心，0.0 = 左上角，1.0 = 右下角
    val normalizedSourceCenterX: Float = 0.5f,

    val normalizedSourceCenterY: Float = 0.5f,

    // 归一化的缩放（相对于 fitScale 的倍数）
    // 1.0 = 适配屏幕大小
    // 2.0 = 放大到适配屏幕的2倍
    val normalizedScale: Float = 1f,

    // 以下属性保持不变
    val rotation: Float = 0f,
    val pivotX: Float = 0.5f,
    val pivotY: Float = 0.5f,
    val alpha: Float = 1f,

    // 元数据：创建时的图片尺寸（用于序列化参考）
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
) {
    companion object {
        /**
         * 创建默认视图状态
         */
        fun default() = ImageViewState(
            normalizedSourceCenterX = 0.5f,
            normalizedSourceCenterY = 0.5f,
            normalizedScale = 1f,
            rotation = 0f,
            pivotX = 0.5f,
            pivotY = 0.5f,
            alpha = 1f,
            imageWidth = 0,
            imageHeight = 0,
        )

        /**
         * 从旧格式 V2 创建（normalizedTranslationX/Y → normalizedSourceCenterX/Y）
         *
         * V2 的 normalizedTranslationX/Y 是 屏幕像素位移 / 图片像素尺寸，
         * 需要反向计算 sourceCenter。
         * 注意：此迁移是近似的，因为 V2 数据在不同横竖屏下本就不一致。
         */
        @Deprecated("仅用于从 V2 数据迁移，新代码请用 default()")
        fun fromV2(
            normalizedTranslationX: Float,
            normalizedTranslationY: Float,
            normalizedScale: Float,
            rotation: Float,
            pivotX: Float,
            pivotY: Float,
            alpha: Float,
            imageWidth: Int,
            imageHeight: Int,
        ): ImageViewState {
            // 无法精确还原 sourceCenter（缺少原始的 viewCenter 和 fitScale），
            // 对于绝大多数场景（居中的图片），translation ≈ 0，迁移效果可接受。
            // normalizedSourceCenter = 0.5 + normalizedTranslation（近似）
            return ImageViewState(
                normalizedSourceCenterX = (0.5f + normalizedTranslationX).coerceIn(0f, 1f),
                normalizedSourceCenterY = (0.5f + normalizedTranslationY).coerceIn(0f, 1f),
                normalizedScale = normalizedScale,
                rotation = rotation,
                pivotX = pivotX,
                pivotY = pivotY,
                alpha = alpha,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
        }

        /**
         * 从旧格式 V1 创建（用于迁移，直接映射旧字段）
         */
        @Deprecated("仅用于从 V1 数据迁移")
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
                normalizedSourceCenterX = 0.5f,
                normalizedSourceCenterY = 0.5f,
                normalizedScale = scaleX,
                rotation = rotation,
                pivotX = pivotX,
                pivotY = pivotY,
                alpha = alpha,
            )
        }
    }
}

/**
 * 从当前 ImageView 状态创建归一化的 ImageViewState
 *
 * @param sourceCenterX SubsamplingScaleImageView 当前的源图片中心X坐标
 * @param sourceCenterY SubsamplingScaleImageView 当前的源图片中心Y坐标
 * @param currentScale 当前实际缩放值
 * @param fitScale 适配屏幕的缩放值
 * @param imageWidth 图片原始宽度
 * @param imageHeight 图片原始高度
 * @param rotation 旋转角度
 * @param pivotX 缩放中心X
 * @param pivotY 缩放中心Y
 * @param alpha 透明度
 */
fun createNormalizedImageViewState(
    sourceCenterX: Float,
    sourceCenterY: Float,
    currentScale: Float,
    fitScale: Float,
    imageWidth: Int,
    imageHeight: Int,
    rotation: Float = 0f,
    pivotX: Float = 0.5f,
    pivotY: Float = 0.5f,
    alpha: Float = 1f,
): ImageViewState {
    return ImageViewState(
        normalizedScale = if (fitScale > 0) currentScale / fitScale else 1f,
        normalizedSourceCenterX = if (imageWidth > 0) sourceCenterX / imageWidth else 0.5f,
        normalizedSourceCenterY = if (imageHeight > 0) sourceCenterY / imageHeight else 0.5f,
        rotation = rotation,
        pivotX = pivotX,
        pivotY = pivotY,
        alpha = alpha,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
    )
}
