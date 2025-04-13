package eu.kanade.tachiyomi.ui.gallery.component

import androidx.annotation.Px
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp

@Composable
fun TagVerticalSelector(
    modifier: Modifier = Modifier,
    @Px transparentBarWidth: Float = 48f, // 渐变区域宽度（单位：像素）
    actions: @Composable (RowScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val backgroundColor = LocalContentColor.current
    // 从左（透明）到右（不透明）的横向渐变画笔
    val edgeGradientBrush = remember(transparentBarWidth) {
        Brush.horizontalGradient(
            colors = listOf(
                backgroundColor, // 右边缘不透明
                backgroundColor.copy(alpha = 0.3f), // 中间半透明
                Color.Transparent, // 左边缘完全透明
            ),
            startX = 0f, // 渐变起始点（左边缘）
            endX = transparentBarWidth, // 渐变结束点（距离左边缘48像素）
            tileMode = TileMode.Clamp,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(end = 300.dp),
            content = content,
        )
        actions?.invoke(this)
    }

    /*when (actions) {
        null -> LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )

        else -> Box(modifier = modifier.fillMaxWidth()) {
            // 标签行：允许横向滚动
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                // .padding(end = transparentBarWidth.pxToDp.dp), // 为操作栏预留空间
                content = content,
            )

            // 右侧操作栏（叠加模式）
            Row(
                modifier = Modifier
                    // .padding(start = transparentBarWidth.pxToDp.dp)
                    .align(Alignment.CenterEnd) // 固定在右侧
     */
    /*.drawWithContent {
                        drawContent() // 先绘制操作按钮
                        drawRect( // 再覆盖左边缘渐变
                            brush = edgeGradientBrush,
                            blendMode = BlendMode.DstIn, // 用渐变Alpha通道裁剪操作栏背景
                        )
                    }*/
    /*
                    .fillMaxHeight(),
            ) {
                actions.invoke(this)
            }
        }
    }*/
}
