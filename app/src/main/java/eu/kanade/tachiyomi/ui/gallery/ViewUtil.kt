package eu.kanade.tachiyomi.ui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.tachiyomi.model.ImageBO
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt

object ViewUtil {
    // Float 扩展函数
    @Composable
    fun Float.dpToPx(): Float {
        val density = LocalDensity.current.density
        return this * density
    }

    @Composable
    fun Float.pxToDp(): Float {
        val density = LocalDensity.current.density
        return this / density
    }

    // Int 扩展函数（常用场景）
    @Composable
    fun Int.dpToPx(): Int = this.toFloat().dpToPx().toInt()

    @Composable
    fun Int.pxToDp(): Int = this.toFloat().pxToDp().toInt()

    @Deprecated("不再使用", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("calculateImageRowV2"))
    tailrec fun calculateImageRow(
        maxHeight: Int,
        minHeight: Int = 200,
        screenWidth: Int,
        spacing: Int = 0,
        maxSize: Int = 8,
        images: List<ImageBO>,
        acc: List<GalleryItem.Images> = emptyList(),
    ): List<GalleryItem.Images> {
        Timber.e("calculateImageRow: maxHeight=$maxHeight, minHeight=$minHeight, screenWidth=$screenWidth, spacing=$spacing, maxSize=$maxSize, images=${images.size}, acc=${acc.size}")
        if (images.isEmpty() || maxSize <= 0) return acc

        for (count in maxSize downTo 1) {
            val takeCount = count - 1
            val currentImages = images.take(takeCount)

            fun findValidHeight(heightRange: IntProgression): GalleryItem.Images? {
                return heightRange.firstOrNull { height ->
                    (
                        abs(
                            calculateImageWidth(
                                currentImages,
                                height,
                                spacing,
                            ) - screenWidth,
                        ) < 10
                        ) || calculateImageWidth(currentImages, maxHeight, spacing) < screenWidth
                }?.let { GalleryItem.Images(currentImages, it) }
            }

            listOf(
                ::findValidHeight to (maxHeight..maxHeight), // 优先尝试maxHeight
                ::findValidHeight to (maxHeight downTo minHeight step 4), // 精细搜索
                ::findValidHeight to (minHeight..maxHeight step 10), // 次选反向步进搜索
            ).firstNotNullOfOrNull { (finder, range) ->
                finder(range)
            }?.let { validItem ->
                return calculateImageRow(
                    maxHeight,
                    minHeight,
                    screenWidth,
                    spacing,
                    maxSize,
                    images.drop(takeCount),
                    acc + validItem,
                )
            }
        }

        return calculateImageRow(
            maxHeight,
            minHeight,
            screenWidth,
            spacing,
            maxSize - 1,
            images,
            acc,
        )
    }

    tailrec fun calculateImageRowV2(
        minHeight: Int = 200,
        maxHeight: Int,
        screenWidth: Int,
        spacing: Int = 0,
        maxSize: Int = 8,
        curSize: Int = maxSize,
        images: List<ImageBO>,
        acc: List<GalleryItem.Images> = emptyList(),
    ): List<GalleryItem.Images> {
        Timber.e("calculateImageRow: minHeight=$minHeight, screenWidth=$screenWidth, spacing=$spacing, maxSize=$maxSize, images=${images.size}, acc=${acc.size}")
        if (images.isEmpty() || curSize <= 0) return acc
        val currentImages = images.take(curSize)

        val ratioSum = currentImages.sumOf { it.aspectRatio.toDouble() }
        val calculateHeight = screenWidth / ratioSum
        if (calculateHeight > minHeight) {
            return calculateImageRowV2(
                minHeight,
                maxHeight,
                screenWidth,
                spacing,
                maxSize,
                maxSize,
                images.drop(curSize),
                acc + GalleryItem.Images(currentImages, calculateHeight.roundToInt()).apply {
                    if (this@apply.images.size <= 3 && height > maxHeight / 1.5) {
                        height = (maxHeight / 1.5).roundToInt()
                    } else if (height > maxHeight) {
                        height = maxHeight
                    }
                },
            )
        } else {
            return calculateImageRowV2(
                minHeight,
                maxHeight,
                screenWidth,
                spacing,
                maxSize,
                curSize - 1,
                images,
                acc,
            )
        }
    }

    // 优化后的宽度计算（防止除零错误）
    private fun calculateImageWidth(images: List<ImageBO>, height: Int, spacing: Int): Int =
        images.sumOf { img ->
            if (img.height == 0) 0 else (height * img.aspectRatio).toInt()
        } + spacing * (images.size - 1)
}
