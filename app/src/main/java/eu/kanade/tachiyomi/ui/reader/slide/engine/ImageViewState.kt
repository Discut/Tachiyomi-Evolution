package eu.kanade.tachiyomi.ui.reader.slide.engine

data class ImageViewState(
    // 视图在水平方向上的平移距离（单位：像素），正值表示向右移动，负值表示向左[1,8](@ref)
    val translationX: Float,

    // 视图在垂直方向上的平移距离（单位：像素），正值表示向下移动，负值表示向上[1,8](@ref)
    val translationY: Float,

    // 视图在水平方向上的缩放比例，1.0为原始大小，>1.0放大，<1.0缩小[1,8](@ref)
    val scaleX: Float,

    // 视图在垂直方向上的缩放比例，规则同scaleX[1,8](@ref)
    val scaleY: Float,

    // 视图的旋转角度（单位：度），正值表示顺时针旋转，负值表示逆时针[1,8](@ref)
    val rotation: Float,

    // 视图变换基准点的X轴坐标（如缩放/旋转中心），支持像素值或百分比（相对于视图左上角）[1,8](@ref)
    val pivotX: Float,

    // 视图变换基准点的Y轴坐标，规则同pivotX[1,8](@ref)
    val pivotY: Float,

    // 视图的透明度，范围0.0（完全透明）到1.0（完全不透明）[1,8](@ref)
    val alpha: Float,
)
