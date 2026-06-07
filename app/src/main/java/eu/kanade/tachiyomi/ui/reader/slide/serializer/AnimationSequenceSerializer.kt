package eu.kanade.tachiyomi.ui.reader.slide.serializer

import eu.kanade.tachiyomi.ui.reader.slide.engine.ImageViewState
import eu.kanade.tachiyomi.ui.reader.slide.engine.KeyFrame
import eu.kanade.tachiyomi.ui.reader.slide.engine.SlideAnimationPath
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * 动画序列 JSON 数据格式
 *
 * Version 1 (旧格式 - 已弃用):
 * {
 *   "version": 1,
 *   "duration": 10000,
 *   "keyFrames": [
 *     {
 *       "timeMs": 0,
 *       "state": {
 *         "translationX": 0, "translationY": 0,
 *         "scaleX": 1, "scaleY": 1,
 *         "rotation": 0, "pivotX": 0.5, "pivotY": 0.5, "alpha": 1
 *       }
 *     }
 *   ]
 * }
 *
 * Version 2 (归一化格式 - 已弃用，有横竖屏不一致问题):
 * {
 *   "version": 2,
 *   "duration": 10000,
 *   "imageWidth": 2000, "imageHeight": 3000,
 *   "keyFrames": [
 *     {
 *       "timeMs": 0,
 *       "state": {
 *         "normalizedTranslationX": 0, "normalizedTranslationY": 0,
 *         "normalizedScale": 1,
 *         "rotation": 0, "pivotX": 0.5, "pivotY": 0.5, "alpha": 1
 *       }
 *     }
 *   ]
 * }
 *
 * Version 3 (源中心坐标 - 当前版本):
 * {
 *   "version": 3,
 *   "duration": 10000,
 *   "imageWidth": 2000, "imageHeight": 3000,
 *   "keyFrames": [
 *     {
 *       "timeMs": 0,
 *       "state": {
 *         "normalizedSourceCenterX": 0.5, "normalizedSourceCenterY": 0.5,
 *         "normalizedScale": 1,
 *         "rotation": 0, "pivotX": 0.5, "pivotY": 0.5, "alpha": 1
 *       }
 *     }
 *   ]
 * }
 */

/**
 * 动画序列序列化工具
 *
 * 提供 SlideAnimationPath 与 JSON 字符串之间的转换功能
 * 支持版本 1（旧格式）、版本 2（归一化位移格式）和版本 3（源中心坐标格式）
 */
object AnimationSequenceSerializer {

    private const val KEY_VERSION = "version"
    private const val KEY_DURATION = "duration"
    private const val KEY_IMAGE_WIDTH = "imageWidth"
    private const val KEY_IMAGE_HEIGHT = "imageHeight"
    private const val KEY_KEY_FRAMES = "keyFrames"
    private const val KEY_TIME_MS = "timeMs"
    private const val KEY_STATE = "state"

    // Version 1 字段（旧格式）
    private const val KEY_TRANSLATION_X = "translationX"
    private const val KEY_TRANSLATION_Y = "translationY"
    private const val KEY_SCALE_X = "scaleX"
    private const val KEY_SCALE_Y = "scaleY"

    // Version 2 字段（归一化位移格式）
    private const val KEY_NORMALIZED_TRANSLATION_X = "normalizedTranslationX"
    private const val KEY_NORMALIZED_TRANSLATION_Y = "normalizedTranslationY"

    // Version 3 字段（源中心坐标格式）
    private const val KEY_NORMALIZED_SOURCE_CENTER_X = "normalizedSourceCenterX"
    private const val KEY_NORMALIZED_SOURCE_CENTER_Y = "normalizedSourceCenterY"

    private const val KEY_NORMALIZED_SCALE = "normalizedScale"
    private const val KEY_ROTATION = "rotation"
    private const val KEY_PIVOT_X = "pivotX"
    private const val KEY_PIVOT_Y = "pivotY"
    private const val KEY_ALPHA = "alpha"

    private const val CURRENT_VERSION = 3

    /**
     * 将 SlideAnimationPath 序列化为 JSON 字符串（版本 3）
     *
     * @param path 动画路径对象
     * @return JSON 字符串
     * @throws SerializationException 序列化失败时抛出
     */
    fun serialize(path: SlideAnimationPath): String {
        return try {
            JSONObject().apply {
                put(KEY_VERSION, CURRENT_VERSION)
                put(KEY_DURATION, path.durationMs)

                // 从第一个关键帧获取图片尺寸
                val firstFrame = path.keyFrames.firstOrNull()
                if (firstFrame != null) {
                    put(KEY_IMAGE_WIDTH, firstFrame.state.imageWidth)
                    put(KEY_IMAGE_HEIGHT, firstFrame.state.imageHeight)
                }

                put(KEY_KEY_FRAMES, serializeKeyFramesV3(path.keyFrames))
            }.toString()
        } catch (e: JSONException) {
            throw SerializationException("Failed to serialize animation path", e)
        }
    }

    /**
     * 将 JSON 字符串反序列化为 SlideAnimationPath
     * 自动检测版本并处理迁移
     *
     * @param json JSON 字符串
     * @return SlideAnimationPath 对象
     * @throws SerializationException 反序列化失败时抛出
     */
    fun deserialize(json: String): SlideAnimationPath {
        return try {
            val jsonObject = JSONObject(json)
            val version = jsonObject.optInt(KEY_VERSION, 1)

            when (version) {
                1 -> migrateFromV1(jsonObject)
                2 -> migrateFromV2(jsonObject)
                3 -> deserializeFromV3(jsonObject)
                else -> throw IllegalArgumentException("Unsupported version: $version")
            }
        } catch (e: JSONException) {
            throw SerializationException("Failed to deserialize animation path", e)
        } catch (e: IllegalArgumentException) {
            throw SerializationException("Invalid animation data", e)
        }
    }

    /**
     * 从版本 1 格式迁移（旧格式 → V3）
     */
    private fun migrateFromV1(jsonObject: JSONObject): SlideAnimationPath {
        Timber.d("Migrating animation sequence from version 1 to version 3")
        val duration = jsonObject.getLong(KEY_DURATION)
        val keyFramesArray = jsonObject.getJSONArray(KEY_KEY_FRAMES)

        val keyFrames = mutableListOf<KeyFrame>()
        for (i in 0 until keyFramesArray.length()) {
            val obj = keyFramesArray.getJSONObject(i)
            val timeMs = obj.getLong(KEY_TIME_MS)
            val stateObj = obj.getJSONObject(KEY_STATE)

            val scale = stateObj.getDouble(KEY_SCALE_X).toFloat()
            val rotation = stateObj.getDouble(KEY_ROTATION).toFloat()
            val pivotX = stateObj.getDouble(KEY_PIVOT_X).toFloat()
            val pivotY = stateObj.getDouble(KEY_PIVOT_Y).toFloat()
            val alpha = stateObj.getDouble(KEY_ALPHA).toFloat()

            // V1 数据使用绝对值，直接映射到 V3 的默认中心位置
            val state = ImageViewState.fromLegacy(
                translationX = stateObj.getDouble(KEY_TRANSLATION_X).toFloat(),
                translationY = stateObj.getDouble(KEY_TRANSLATION_Y).toFloat(),
                scaleX = scale,
                scaleY = stateObj.getDouble(KEY_SCALE_Y).toFloat(),
                rotation = rotation,
                pivotX = pivotX,
                pivotY = pivotY,
                alpha = alpha,
            )

            keyFrames.add(KeyFrame(timeMs, state))
        }

        return SlideAnimationPath(
            durationMs = duration,
            keyFrames = keyFrames,
        )
    }

    /**
     * 从版本 2 格式迁移（归一化位移 → V3 源中心坐标）
     *
     * V2 的 normalizedTranslationX/Y 是屏幕像素位移 / 图片尺寸，
     * V3 改为 normalizedSourceCenterX/Y 是源图片中心坐标 / 图片尺寸。
     * 迁移使用近似映射：normalizedSourceCenter = 0.5 + normalizedTranslation
     * 注意：迁移后的 V2 数据可能与原始横竖屏效果有差异，建议用户重新编辑。
     */
    private fun migrateFromV2(jsonObject: JSONObject): SlideAnimationPath {
        Timber.d("Migrating animation sequence from version 2 to version 3")
        val duration = jsonObject.getLong(KEY_DURATION)
        val imageWidth = jsonObject.optInt(KEY_IMAGE_WIDTH, 0)
        val imageHeight = jsonObject.optInt(KEY_IMAGE_HEIGHT, 0)
        val keyFramesArray = jsonObject.getJSONArray(KEY_KEY_FRAMES)

        val keyFrames = mutableListOf<KeyFrame>()
        for (i in 0 until keyFramesArray.length()) {
            val obj = keyFramesArray.getJSONObject(i)
            val timeMs = obj.getLong(KEY_TIME_MS)
            val stateObj = obj.getJSONObject(KEY_STATE)

            val state = ImageViewState.fromV2(
                normalizedTranslationX = stateObj.getDouble(KEY_NORMALIZED_TRANSLATION_X).toFloat(),
                normalizedTranslationY = stateObj.getDouble(KEY_NORMALIZED_TRANSLATION_Y).toFloat(),
                normalizedScale = stateObj.getDouble(KEY_NORMALIZED_SCALE).toFloat(),
                rotation = stateObj.getDouble(KEY_ROTATION).toFloat(),
                pivotX = stateObj.getDouble(KEY_PIVOT_X).toFloat(),
                pivotY = stateObj.getDouble(KEY_PIVOT_Y).toFloat(),
                alpha = stateObj.getDouble(KEY_ALPHA).toFloat(),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )

            keyFrames.add(KeyFrame(timeMs, state))
        }

        return SlideAnimationPath(
            durationMs = duration,
            keyFrames = keyFrames,
        )
    }

    /**
     * 从版本 3 格式反序列化（源中心坐标格式）
     */
    private fun deserializeFromV3(jsonObject: JSONObject): SlideAnimationPath {
        val duration = jsonObject.getLong(KEY_DURATION)
        val imageWidth = jsonObject.optInt(KEY_IMAGE_WIDTH, 0)
        val imageHeight = jsonObject.optInt(KEY_IMAGE_HEIGHT, 0)
        val keyFramesArray = jsonObject.getJSONArray(KEY_KEY_FRAMES)

        val keyFrames = deserializeKeyFramesV3(keyFramesArray, imageWidth, imageHeight)

        return SlideAnimationPath(
            durationMs = duration,
            keyFrames = keyFrames,
        )
    }

    /**
     * 序列化关键帧列表为 JSONArray（版本 3）
     */
    private fun serializeKeyFramesV3(keyFrames: List<KeyFrame>): JSONArray {
        return JSONArray().apply {
            keyFrames.forEach { frame ->
                put(
                    JSONObject().apply {
                        put(KEY_TIME_MS, frame.timeMs)
                        put(KEY_STATE, serializeImageViewStateV3(frame.state))
                    },
                )
            }
        }
    }

    /**
     * 从 JSONArray 反序列化关键帧列表（版本 3）
     */
    private fun deserializeKeyFramesV3(
        array: JSONArray,
        imageWidth: Int,
        imageHeight: Int,
    ): List<KeyFrame> {
        val keyFrames = mutableListOf<KeyFrame>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val timeMs = obj.getLong(KEY_TIME_MS)
            val stateObj = obj.getJSONObject(KEY_STATE)

            val state = deserializeImageViewStateV3(stateObj, imageWidth, imageHeight)

            keyFrames.add(KeyFrame(timeMs, state))
        }
        return keyFrames
    }

    /**
     * 序列化 ImageViewState 为 JSONObject（版本 3）
     */
    private fun serializeImageViewStateV3(state: ImageViewState): JSONObject {
        return JSONObject().apply {
            put(KEY_NORMALIZED_SOURCE_CENTER_X, state.normalizedSourceCenterX.toDouble())
            put(KEY_NORMALIZED_SOURCE_CENTER_Y, state.normalizedSourceCenterY.toDouble())
            put(KEY_NORMALIZED_SCALE, state.normalizedScale)
            put(KEY_ROTATION, state.rotation)
            put(KEY_PIVOT_X, state.pivotX)
            put(KEY_PIVOT_Y, state.pivotY)
            put(KEY_ALPHA, state.alpha)
        }
    }

    /**
     * 从 JSONObject 反序列化 ImageViewState（版本 3）
     */
    private fun deserializeImageViewStateV3(
        obj: JSONObject,
        imageWidth: Int,
        imageHeight: Int,
    ): ImageViewState {
        return ImageViewState(
            normalizedSourceCenterX = obj.getDouble(KEY_NORMALIZED_SOURCE_CENTER_X).toFloat(),
            normalizedSourceCenterY = obj.getDouble(KEY_NORMALIZED_SOURCE_CENTER_Y).toFloat(),
            normalizedScale = obj.getDouble(KEY_NORMALIZED_SCALE).toFloat(),
            rotation = obj.getDouble(KEY_ROTATION).toFloat(),
            pivotX = obj.getDouble(KEY_PIVOT_X).toFloat(),
            pivotY = obj.getDouble(KEY_PIVOT_Y).toFloat(),
            alpha = obj.getDouble(KEY_ALPHA).toFloat(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )
    }

    /**
     * 验证版本兼容性
     */
    private fun validateVersion(version: Int) {
        if (version > CURRENT_VERSION) {
            throw IllegalArgumentException(
                "Unsupported version: $version (current: $CURRENT_VERSION)",
            )
        }
    }

    /**
     * 验证 JSON 数据格式是否有效
     */
    fun validate(json: String): Boolean {
        return try {
            val jsonObject = JSONObject(json)
            val version = jsonObject.optInt(KEY_VERSION, 1)
            val duration = jsonObject.optLong(KEY_DURATION, -1)
            val keyFramesArray = jsonObject.optJSONArray(KEY_KEY_FRAMES)

            validateVersion(version)
            require(duration > 0) { "Duration must be positive" }
            require(keyFramesArray != null && keyFramesArray.length() > 0) { "Key frames cannot be empty" }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 序列化异常
     */
    class SerializationException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
