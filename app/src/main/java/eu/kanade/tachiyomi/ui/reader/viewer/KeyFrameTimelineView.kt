package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.use
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.slide.engine.KeyFrame
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class KeyFrameTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    // ==================== 属性声明 ====================

    // 数据接口
    var adapter: TimelineAdapter? = null
        set(value) {
            val wasNull = field == null
            field = value

            // adapter 改变时触发刷新
            invalidate()

            // 如果是从 null 变为非 null（首次设置），滚动到当前时间
            if (wasNull && value != null) {
                seekToTime(value.getCurrentPosition())
            }
        }

    // 颜色定义（适配主题）
    private var trackColor: Int
    private var trackFillColor: Int
    private var cursorColor: Int
    private var keyFrameColor: Int
    private var keyFrameSelectedColor: Int
    private var textColor: Int
    private var backgroundColor: Int = 0

    // 布局参数
    private val dp2: Float
    private val dp3: Float
    private val dp4: Float
    private val dp6: Float
    private val dp8: Float
    private val dp10: Float
    private val dp12: Float
    private val dp16: Float
    private val dp20: Float
    private val dp24: Float
    private val dp32: Float
    private val dp48: Float
    private val dp72: Float
    private val sp12: Float
    private val sp14: Float

    // 关键帧选中状态
    private var selectedKeyFrameIndex: Int? = null

    companion object {
        private const val TAG = "KeyFrameTimeline"
        private const val COLLISION_THRESHOLD_MS = 100L
        private const val INTERACTION_COOLDOWN_MS = 500L
    }

    // ==================== 触摸状态机 ====================

    private var touchState: TouchState = TouchState.Idle

    private sealed class TouchState {
        object Idle : TouchState()
        object DraggingCursor : TouchState()
        data class DraggingKeyFrame(
            val timeMs: Long, // 当前关键帧时间（用于排序后重定位，会随拖动更新）
            val startTimeMs: Long, // 拖动起点时间（固定不变，计算 virtualTimeMs 的基准）
            val startRawX: Float, // 手指起点 x 坐标（固定不变）
        ) : TouchState()
    }

    private var isUserInteracting = false // 交互锁：阻止 seekToTime 在交互/冷却期内更新光标
    private var touchedSelectedKeyFrame = false // onDown→onScroll 过渡标记

    // 光标位置
    private var cursorX: Float = 0f // 光标在轨道上的相对位置 (0 ~ trackWidth)
    private val minCursorX: Float get() = 0f
    private val maxCursorX: Float get() = trackWidth

    // 轨道宽度（绘制轨道的实际宽度）
    private var trackWidth: Float = 0f

    // 记录上一次的内容宽度，用于尺寸变化时保持相对进度
    private var prevTrackWidth: Float = 0f

    // 交互冷却
    private var resetInteractionRunnable: Runnable? = null

    // 光标触摸半径
    private var cursorTouchRadius = 0f

    // 查找关键帧的触摸半径（延迟初始化，因为 dp24 在 init 中计算）
    private val keyFrameTouchRadius: Float
        get() = dp24

    // ==================== Paint 对象 ====================

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }
    private val trackFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val keyFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val keyFrameStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val keyFrameGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val cursorGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    // ==================== 初始化 ====================

    init {
        // 解析 XML 属性
        context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.background)).use { a ->
            // 检查是否在 XML 中设置了背景
            if (a.hasValue(0)) {
                // 使用 XML 中设置的背景
                val backgroundDrawable = a.getDrawable(0)
                background = backgroundDrawable
                // 从背景drawable推断背景颜色（如果可能）
                backgroundColor = if (backgroundDrawable != null) {
                    extractColorFromDrawable(backgroundDrawable)
                } else {
                    getDefaultBackgroundColor()
                }
            } else {
                // 未设置背景，保持透明（不设置 backgroundColor）
                backgroundColor = 0
            }
        }

        // 解析主题颜色
        val typedValue = TypedValue()
        val theme = context.theme

        // 获取轨道颜色
        trackColor = if (isColorDark(backgroundColor)) {
            "#424242".toColorInt()
        } else {
            "#E0E0E0".toColorInt()
        }

        trackFillColor = if (theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            "#2979FF".toColorInt()
        }

        cursorColor = if (theme.resolveAttribute(R.attr.colorSecondary, typedValue, true)) {
            typedValue.data
        } else {
            "#FF4081".toColorInt()
        }

        keyFrameColor = if (theme.resolveAttribute(R.attr.colorOnPrimary, typedValue, true)) {
            typedValue.data
        } else {
            Color.WHITE
        }

        keyFrameSelectedColor = trackFillColor

        textColor = if (theme.resolveAttribute(R.attr.colorOnBackground, typedValue, true)) {
            typedValue.data
        } else {
            Color.WHITE
        }

        // 转换 dp/sp 到 px
        dp2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
        dp3 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics)
        dp4 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
        dp6 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics)
        dp8 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)
        dp10 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics)
        dp12 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)
        dp16 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
        dp20 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics)
        dp24 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
        dp32 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32f, resources.displayMetrics)
        dp48 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics)
        dp72 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72f, resources.displayMetrics)
        sp12 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        sp14 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)

        // 光标触摸半径
        cursorTouchRadius = dp32

        // 初始化 Paint 颜色
        trackPaint.color = trackColor
        keyFramePaint.color = keyFrameColor
        keyFrameStrokePaint.color = trackFillColor
        cursorPaint.color = cursorColor
        cursorGlowPaint.color = cursorColor
        textPaint.color = textColor
        textPaint.textSize = sp12
    }

    // ==================== 手势处理 ====================

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                isUserInteracting = true

                // 记录手指是否按在已选中的关键帧上（不改变选中状态）
                val hitIndex = findKeyFrameAtPosition(e.x, e.y)
                touchedSelectedKeyFrame = hitIndex != null && hitIndex == selectedKeyFrameIndex

                // 检查是否点击了光标区域，预判可能的光标拖动
                val drawHeight = height - paddingTop - paddingBottom
                val centerY = drawHeight / 2f + paddingTop
                val currentCursorScreenX = paddingLeft + trackPadding + cursorX
                val distanceToCursor = sqrt(
                    (e.x - currentCursorScreenX).pow(2) + (e.y - centerY).pow(2),
                )
                if (distanceToCursor <= cursorTouchRadius) {
                    touchState = TouchState.DraggingCursor
                }

                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                val adapter = adapter ?: return false
                val durationMs = adapter.getDuration()
                if (durationMs <= 0 || trackWidth <= 0) return false

                when (val state = touchState) {
                    // === 关键帧拖动中 ===
                    is TouchState.DraggingKeyFrame -> {
                        val keyFrames = adapter.getKeyFrames()
                        val currentIndex = keyFrames.indexOfFirst { it.timeMs == state.timeMs }
                        if (currentIndex < 0) return false

                        val timePerPixel = durationMs.toFloat() / trackWidth
                        val totalDx = e2.x - state.startRawX
                        val virtualTimeMs = (state.startTimeMs + totalDx * timePerPixel)
                            .toLong()
                            .coerceIn(0L, durationMs)

                        val collides = keyFrames.any { other ->
                            other !== keyFrames[currentIndex] &&
                                abs(other.timeMs - virtualTimeMs) < COLLISION_THRESHOLD_MS
                        }
                        if (collides) {
                            Timber.d( "keyframe collision, virtualTime=$virtualTimeMs frozen, keys=${keyFrames.map { it.timeMs }}")
                            ViewCompat.postInvalidateOnAnimation(this@KeyFrameTimelineView)
                            return true
                        }

                        adapter.onKeyFrameTimeChanged(currentIndex, virtualTimeMs)
                        touchState = state.copy(timeMs = virtualTimeMs)

                        val newIndex = adapter.getKeyFrames().indexOfFirst { it.timeMs == virtualTimeMs }
                        if (newIndex >= 0) {
                            selectedKeyFrameIndex = newIndex
                        }

                        ViewCompat.postInvalidateOnAnimation(this@KeyFrameTimelineView)
                        return true
                    }

                    // === 光标拖动中 ===
                    is TouchState.DraggingCursor -> {
                        cursorX -= distanceX
                        clampCursor()
                        updateCurrentTimeFromCursor()
                        ViewCompat.postInvalidateOnAnimation(this@KeyFrameTimelineView)
                        return true
                    }

                    // === Idle → 判断是否启动拖动 ===
                    is TouchState.Idle -> {
                        // 手指按在已选中关键帧上 → 启动关键帧拖动
                        if (touchedSelectedKeyFrame && selectedKeyFrameIndex != null) {
                            val keyFrames = adapter.getKeyFrames()
                            val kfTimeMs = keyFrames.getOrNull(selectedKeyFrameIndex!!)?.timeMs ?: return false
                            touchState = TouchState.DraggingKeyFrame(
                                timeMs = kfTimeMs,
                                startTimeMs = kfTimeMs,
                                startRawX = e2.x,
                            )
                            touchedSelectedKeyFrame = false
                            Timber.d( "start dragging keyframe at ${kfTimeMs}ms, startRawX=${e2.x}")
                            return true
                        }

                        // 水平滑动 → 光标拖动（onDown 未命中光标但滑动意图明确）
                        val isHorizontalScroll = abs(distanceX) > abs(distanceY) * 1.5f && abs(distanceX) > 8f
                        if (isHorizontalScroll && e1 != null) {
                            val xInPadding = e1.x - paddingLeft
                            val newCursorX = (xInPadding - trackPadding).coerceIn(0f, trackWidth)
                            cursorX = newCursorX
                            isUserInteracting = true
                            touchState = TouchState.DraggingCursor
                            return true
                        }
                        return false
                    }
                }
            }

            override fun onLongPress(e: MotionEvent) {
                handleKeyFrameLongPress(e.x, e.y)
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // 拖动结束后的松手不算点击
                if (touchState != TouchState.Idle) {
                    touchedSelectedKeyFrame = false
                    postInteractionCooldown()
                    return super.onSingleTapUp(e)
                }

                val hitIndex = findKeyFrameAtPosition(e.x, e.y)
                if (hitIndex != null) {
                    if (selectedKeyFrameIndex == hitIndex) {
                        // 再次点击已选中关键帧 → 取消选中
                        selectedKeyFrameIndex = null
                        adapter?.onKeyFrameTouchSelected(null)
                    } else {
                        // 选中新关键帧
                        selectedKeyFrameIndex = hitIndex
                        adapter?.onKeyFrameSelected(hitIndex)
                        adapter?.onKeyFrameTouchSelected(hitIndex)
                    }
                    invalidate()
                } else {
                    // 点击轨道空白处 → 跳转光标
                    val drawHeight = height - paddingTop - paddingBottom
                    val centerY = drawHeight / 2f + paddingTop
                    if (abs(e.y - centerY) < dp24) {
                        handleTrackTap(e.x)
                    }
                }

                touchedSelectedKeyFrame = false
                postInteractionCooldown()
                return super.onSingleTapUp(e)
            }
        },
    )

    // ==================== View 生命周期 ====================

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 获取 padding
        val paddingX = paddingLeft + paddingRight
        val paddingY = paddingTop + paddingBottom

        // 期望的最小尺寸（考虑 padding）
        val desiredWidth = dp48 + paddingX
        val desiredHeight = dp72 + paddingY

        // 计算宽度
        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> min(desiredWidth.toInt(), MeasureSpec.getSize(widthMeasureSpec))
            MeasureSpec.UNSPECIFIED -> desiredWidth.toInt()
            else -> desiredWidth.toInt()
        }

        // 计算高度
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(desiredHeight.toInt(), MeasureSpec.getSize(heightMeasureSpec))
            MeasureSpec.UNSPECIFIED -> desiredHeight.toInt()
            else -> desiredHeight.toInt()
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 计算当前进度比例
        val currentProgress = if (prevTrackWidth > 0) {
            cursorX / prevTrackWidth
        } else {
            0.5f // 默认在中间
        }

        // 计算可用宽度（减去 padding 和轨道左右间距）
        val availableWidth = width - paddingLeft - paddingRight
        trackPadding = dp24
        trackWidth = availableWidth - trackPadding * 2

        // 保持相对进度
        if (prevTrackWidth > 0) {
            cursorX = currentProgress * trackWidth
        } else {
            cursorX = trackWidth * 0.5f
        }

        prevTrackWidth = trackWidth
        clampCursor()
        invalidate()
    }

    // ==================== 绘制相关 ====================

    private var trackPadding = 0f

    override fun onDraw(canvas: Canvas) {
        // 应用 padding
        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())

        val drawWidth = width - paddingLeft - paddingRight
        val drawHeight = height - paddingTop - paddingBottom

        // 背景已由 View 系统处理，这里不再绘制
        // 如果 backgroundColor 为 0，表示未设置背景，保持透明
        if (background == null && backgroundColor != 0) {
            val bgRect = RectF(0f, 0f, drawWidth.toFloat(), drawHeight.toFloat())
            trackFillPaint.color = backgroundColor
            trackFillPaint.style = Paint.Style.FILL
            trackFillPaint.alpha = 255
            canvas.drawRoundRect(bgRect, dp8, dp8, trackFillPaint)
        }

        val adapter = adapter ?: return

        val centerX = trackPadding + cursorX // 光标在轨道上的相对位置
        val centerY = drawHeight / 2f
        val trackHeight = dp6
        val trackTop = centerY - trackHeight / 2
        val trackBottom = centerY + trackHeight / 2

        val durationMs = adapter.getDuration().toFloat()
        if (durationMs <= 0) return

        // 当前时间对应的进度 (0.0 ~ 1.0)
        val currentProgress = cursorX / trackWidth

        // ==================== 1. 绘制背景轨道 ====================
        trackFillPaint.color = trackColor
        val bgTrackRect = RectF(
            trackPadding,
            trackTop,
            trackPadding + trackWidth,
            trackBottom,
        )
        canvas.drawRoundRect(bgTrackRect, trackHeight / 2, trackHeight / 2, trackFillPaint)

        // ==================== 2. 绘制已播放的轨道（带渐变） ====================
        val playedWidth = cursorX
        val playedRect = RectF(
            trackPadding,
            trackTop,
            trackPadding + playedWidth,
            trackBottom,
        )

        if (drawWidth > 0 && drawHeight > 0) {
            val gradient = LinearGradient(
                playedRect.left,
                trackTop,
                playedRect.right,
                trackBottom,
                intArrayOf(
                    trackFillColor,
                    adjustAlpha(trackFillColor, 0.7f),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
            trackFillPaint.shader = gradient
        }
        trackFillPaint.color = trackFillColor
        canvas.drawRoundRect(playedRect, trackHeight / 2, trackHeight / 2, trackFillPaint)
        trackFillPaint.shader = null

        // ==================== 3. 绘制轨道分隔线 ====================
        trackPaint.strokeWidth = 1f
        trackPaint.color = if (isColorDark(trackFillColor)) Color.WHITE else Color.BLACK
        trackPaint.alpha = 50
        val timeMarkers = listOf(0.25f, 0.5f, 0.75f)
        timeMarkers.forEach { ratio ->
            val markerX = trackPadding + ratio * trackWidth
            if (markerX in trackPadding..trackPadding + trackWidth) {
                canvas.drawLine(
                    markerX,
                    trackTop - dp6,
                    markerX,
                    trackBottom + dp6,
                    trackPaint,
                )
                val timeText = formatTime((ratio * durationMs).toLong())
                canvas.drawText(timeText, markerX, trackBottom + dp20, textPaint)
            }
        }
        trackPaint.alpha = 255
        trackPaint.strokeWidth = 4f

        // ==================== 4. 绘制关键帧 ====================
        val keyFrames = adapter.getKeyFrames()
        for (i in keyFrames.indices) {
            val timeMs = keyFrames[i].timeMs
            val ratio = timeMs / durationMs
            val drawX = trackPadding + ratio * trackWidth

            if (drawX in -dp32..drawWidth + dp32) {
                val isSelected = selectedKeyFrameIndex == i
                val keyFrameRadius = if (isSelected) dp10 else dp6

                keyFrameGlowPaint.color = cursorColor
                keyFrameGlowPaint.alpha = 40
                canvas.drawCircle(drawX, centerY, keyFrameRadius + dp3, keyFrameGlowPaint)
                keyFrameGlowPaint.alpha = 255

                keyFramePaint.color = if (isSelected) keyFrameSelectedColor else trackFillColor
                canvas.drawCircle(drawX, centerY, keyFrameRadius, keyFramePaint)

                keyFrameStrokePaint.color = if (isSelected) Color.WHITE else trackFillColor
                canvas.drawCircle(drawX, centerY, keyFrameRadius, keyFrameStrokePaint)

                if (isSelected) {
                    textPaint.textSize = sp12
                    textPaint.color = Color.WHITE
                    canvas.drawText("${(timeMs / 1000f).format(1)}s", drawX, centerY - dp16, textPaint)
                }
            }
        }

        // ==================== 5. 绘制现代风格光标 ====================

        // 光标阴影/发光效果
        cursorGlowPaint.color = cursorColor
        cursorGlowPaint.alpha = 20
        canvas.drawCircle(centerX, centerY, dp8, cursorGlowPaint)

        // 垂直指示线（上半部分）
        cursorPaint.color = cursorColor
        cursorPaint.style = Paint.Style.STROKE
        cursorPaint.strokeWidth = dp2
        cursorPaint.alpha = 180
        canvas.drawLine(centerX, trackTop - dp12, centerX, centerY - dp4, cursorPaint)

        // 垂直指示线（下半部分）
        cursorPaint.strokeWidth = dp2
        canvas.drawLine(centerX, centerY + dp4, centerX, trackBottom + dp12, cursorPaint)
        cursorPaint.alpha = 255

        // 中心圆点
        val cursorRadius = dp6
        cursorPaint.style = Paint.Style.FILL
        cursorPaint.color = cursorColor
        canvas.drawCircle(centerX, centerY, cursorRadius, cursorPaint)

        // 内部白色高光
        cursorPaint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, dp2, cursorPaint)

        // 外圈光晕
        cursorGlowPaint.color = cursorColor
        cursorGlowPaint.alpha = 40
        canvas.drawCircle(centerX, centerY, cursorRadius + dp3, cursorGlowPaint)
        cursorGlowPaint.alpha = 255

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                touchedSelectedKeyFrame = false
                if (touchState != TouchState.Idle) {
                    postInteractionCooldown()
                }
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 取消所有待执行的 runnable，防止内存泄漏
        resetInteractionRunnable?.let { removeCallbacks(it) }
        resetInteractionRunnable = null
    }

    // ==================== 私有方法 ====================

    private fun clampCursor() {
        cursorX = cursorX.coerceIn(minCursorX, maxCursorX)
    }

    private fun updateCurrentTimeFromCursor() {
        val adapter = adapter ?: return
        val durationMs = adapter.getDuration().toFloat()
        if (durationMs <= 0) return

        val progress = cursorX / trackWidth
        val currentTime = (progress * durationMs).toLong().coerceIn(0, adapter.getDuration().toLong())

        val keyFrames = adapter.getKeyFrames()
        val nearestIndex = keyFrames.indices.minByOrNull { abs(keyFrames[it].timeMs - currentTime) }
        selectedKeyFrameIndex = nearestIndex?.takeIf { abs(keyFrames[it].timeMs - currentTime) < COLLISION_THRESHOLD_MS }

        adapter.onSeekTo(currentTime)
    }

    private fun handleTrackTap(x: Float) {
        // 转换触摸坐标到 padding 内部坐标
        val xInPadding = x - paddingLeft
        // 计算相对于轨道的位置（减去轨道左右间距）
        val newCursorX = (xInPadding - trackPadding).coerceIn(0f, trackWidth)
        cursorX = newCursorX
        clampCursor()
        updateCurrentTimeFromCursor()
        invalidate()
    }

    /**
     * 处理长按关键帧事件
     * @param x 触摸点的 x 坐标
     * @param y 触摸点的 y 坐标
     */
    private fun handleKeyFrameLongPress(x: Float, y: Float) {
        val adapter = adapter ?: return
        val keyFrames = adapter.getKeyFrames()
        if (keyFrames.isEmpty()) return

        val durationMs = adapter.getDuration().toFloat()
        if (durationMs <= 0) return

        val drawHeight = height - paddingTop - paddingBottom
        val centerY = drawHeight / 2f + paddingTop

        for (i in keyFrames.indices) {
            val ratio = keyFrames[i].timeMs / durationMs
            val drawX = paddingLeft + trackPadding + ratio * trackWidth
            val distance = sqrt((x - drawX).pow(2) + (y - centerY).pow(2))

            if (distance <= keyFrameTouchRadius) {
                adapter.onKeyFrameLongPress(i)
                return
            }
        }
    }

    /**
     * 查找触摸位置附近的关键帧
     * @return 关键帧在列表中的 index，如果未找到则返回 null
     */
    private fun findKeyFrameAtPosition(x: Float, y: Float): Int? {
        val adapter = adapter ?: return null
        val keyFrames = adapter.getKeyFrames()
        if (keyFrames.isEmpty()) return null

        val durationMs = adapter.getDuration().toFloat()
        if (durationMs <= 0) return null

        val drawHeight = height - paddingTop - paddingBottom
        val centerY = drawHeight / 2f + paddingTop

        for (i in keyFrames.indices) {
            val ratio = keyFrames[i].timeMs / durationMs
            val drawX = paddingLeft + trackPadding + ratio * trackWidth

            val distance = sqrt((x - drawX).pow(2) + (y - centerY).pow(2))

            if (distance <= keyFrameTouchRadius) {
                return i
            }
        }
        return null
    }

    private fun postInteractionCooldown() {
        // 延迟重置 touchState 和交互锁，阻止此期间的 seekToTime
        resetInteractionRunnable?.let { removeCallbacks(it) }
        resetInteractionRunnable = Runnable {
            touchState = TouchState.Idle
            isUserInteracting = false
        }
        postDelayed(resetInteractionRunnable, INTERACTION_COOLDOWN_MS)
    }

    // ==================== 公共方法 ====================

    /**
     * 获取当前光标对应的时间值
     */
    fun getCurrentTime(): Long {
        val adapter = adapter ?: return 0L
        val durationMs = adapter.getDuration().toFloat()
        if (durationMs <= 0) return 0L

        val progress = cursorX / trackWidth
        return (progress * durationMs).toLong().coerceIn(0, adapter.getDuration().toLong())
    }

    fun seekToTime(timeMs: Long) {
        if (isUserInteracting) {
            return
        }

        val adapter = adapter ?: return
        val durationMs = adapter.getDuration().toFloat()
        if (durationMs <= 0) return

        val progress = (timeMs / durationMs).coerceIn(0f, 1f)
        cursorX = progress * trackWidth.coerceAtLeast(0f)
        clampCursor()
        invalidate()
    }

    /**
     * 选中指定时间的关键帧（高亮并移动光标到该关键帧位置）
     * @param timeMs 关键帧时间，传 null 取消选中
     */
    fun selectKeyFrame(timeMs: Long?) {
        if (timeMs != null) {
            val keyFrames = adapter?.getKeyFrames() ?: emptyList()
            val index = keyFrames.indexOfFirst { it.timeMs == timeMs }
            selectedKeyFrameIndex = if (index >= 0) index else null
            seekToTime(timeMs)
        } else {
            selectedKeyFrameIndex = null
            invalidate()
        }
    }

    // ==================== 辅助方法 ====================

    private fun formatTime(ms: Long): String {
        return "${(ms / 1000f).format(1)}s"
    }

    private fun Float.format(digits: Int): String {
        return "%.${digits}f".format(this)
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun getDefaultBackgroundColor(): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
            typedValue.data
        } else {
            Color.parseColor("#1B1B1F")
        }
    }

    private fun extractColorFromDrawable(drawable: android.graphics.drawable.Drawable): Int {
        // 尝试从 ColorDrawable 提取颜色
        if (drawable is android.graphics.drawable.ColorDrawable) {
            return drawable.color
        }
        // 尝试从 RippleDrawable 或其他类型的 drawable 获取颜色
        return getDefaultBackgroundColor()
    }

    // ==================== 接口定义 ====================

    interface TimelineAdapter {
        fun getDuration(): Long
        fun getCurrentPosition(): Long
        fun getKeyFrames(): List<KeyFrame>
        fun onSeekTo(position: Long)

        /**
         * 点击关键帧圆点时调用
         * @param index 关键帧在列表中的 index
         */
        fun onKeyFrameSelected(index: Int) {}

        /**
         * 长按关键帧时调用，用于打开编辑面板
         * @param index 关键帧在列表中的 index
         */
        fun onKeyFrameLongPress(index: Int) {}

        /**
         * 拖拽关键帧到新时间位置时调用
         * @param index 关键帧在列表中的 index
         * @param newTimeMs 新时间
         */
        fun onKeyFrameTimeChanged(index: Int, newTimeMs: Long) {}

        /**
         * 触摸选中/取消选中关键帧时调用（通知外部更新状态显示）
         * @param index 关键帧在列表中的 index，null 表示取消选中
         */
        fun onKeyFrameTouchSelected(index: Int?) {}
    }
}
