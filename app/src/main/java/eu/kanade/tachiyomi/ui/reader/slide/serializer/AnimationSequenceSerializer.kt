package eu.kanade.tachiyomi.ui.reader.slide.serializer

import eu.kanade.tachiyomi.ui.reader.slide.engine.ImageViewState
import eu.kanade.tachiyomi.ui.reader.slide.engine.KeyFrame
import eu.kanade.tachiyomi.ui.reader.slide.engine.SlideAnimationPath
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 动画序列 JSON 数据格式
 *
 * {
 *   "version": 1,
 *   "duration": 10000,
 *   "keyFrames": [
 *     {
 *       "timeMs": 0,
 *       "state": {
 *         "translationX": 0,
 *         "translationY": 0,
 *         "scaleX": 1,
 *         "scaleY": 1,
 *         "rotation": 0,
 *         "pivotX": 0.5,
 *         "pivotY": 0.5,
 *         "alpha": 1
 *       }
 *     }
 *   ]
 * }
 */

/**
 * 动画序列序列化工具
 *
 * 提供 SlideAnimationPath 与 JSON 字符串之间的转换功能
 */
object AnimationSequenceSerializer {

    private const val KEY_VERSION = "version"
    private const val KEY_DURATION = "duration"
    private const val KEY_KEY_FRAMES = "keyFrames"
    private const val KEY_TIME_MS = "timeMs"
    private const val KEY_STATE = "state"
    private const val KEY_TRANSLATION_X = "translationX"
    private const val KEY_TRANSLATION_Y = "translationY"
    private const val KEY_SCALE_X = "scaleX"
    private const val KEY_SCALE_Y = "scaleY"
    private const val KEY_ROTATION = "rotation"
    private const val KEY_PIVOT_X = "pivotX"
    private const val KEY_PIVOT_Y = "pivotY"
    private const val KEY_ALPHA = "alpha"

    private const val CURRENT_VERSION = 1

    /**
     * 将 SlideAnimationPath 序列化为 JSON 字符串
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
                put(KEY_KEY_FRAMES, serializeKeyFrames(path.keyFrames))
            }.toString()
        } catch (e: JSONException) {
            throw SerializationException("Failed to serialize animation path", e)
        }
    }

    /**
     * 将 JSON 字符串反序列化为 SlideAnimationPath
     *
     * @param json JSON 字符串
     * @return SlideAnimationPath 对象
     * @throws SerializationException 反序列化失败时抛出
     */
    fun deserialize(json: String): SlideAnimationPath {
        return try {
            val jsonObject = JSONObject(json)
            val version = jsonObject.optInt(KEY_VERSION, 1)
            val duration = jsonObject.getLong(KEY_DURATION)
            val keyFramesArray = jsonObject.getJSONArray(KEY_KEY_FRAMES)

            val keyFrames = deserializeKeyFrames(keyFramesArray)

            validateVersion(version)

            SlideAnimationPath(
                durationMs = duration,
                keyFrames = keyFrames,
            )
        } catch (e: JSONException) {
            throw SerializationException("Failed to deserialize animation path", e)
        } catch (e: IllegalArgumentException) {
            throw SerializationException("Invalid animation data", e)
        }
    }

    /**
     * 序列化关键帧列表为 JSONArray
     */
    private fun serializeKeyFrames(keyFrames: List<KeyFrame>): JSONArray {
        return JSONArray().apply {
            keyFrames.forEach { frame ->
                put(
                    JSONObject().apply {
                        put(KEY_TIME_MS, frame.timeMs)
                        put(KEY_STATE, serializeImageViewState(frame.state))
                    },
                )
            }
        }
    }

    /**
     * 从 JSONArray 反序列化关键帧列表
     */
    private fun deserializeKeyFrames(array: JSONArray): List<KeyFrame> {
        val keyFrames = mutableListOf<KeyFrame>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val timeMs = obj.getLong(KEY_TIME_MS)
            val stateObj = obj.getJSONObject(KEY_STATE)

            val state = deserializeImageViewState(stateObj)

            keyFrames.add(KeyFrame(timeMs, state))
        }
        return keyFrames
    }

    /**
     * 序列化 ImageViewState 为 JSONObject
     */
    private fun serializeImageViewState(state: ImageViewState): JSONObject {
        return JSONObject().apply {
            put(KEY_TRANSLATION_X, state.translationX)
            put(KEY_TRANSLATION_Y, state.translationY)
            put(KEY_SCALE_X, state.scaleX)
            put(KEY_SCALE_Y, state.scaleY)
            put(KEY_ROTATION, state.rotation)
            put(KEY_PIVOT_X, state.pivotX)
            put(KEY_PIVOT_Y, state.pivotY)
            put(KEY_ALPHA, state.alpha)
        }
    }

    /**
     * 从 JSONObject 反序列化 ImageViewState
     */
    private fun deserializeImageViewState(obj: JSONObject): ImageViewState {
        return ImageViewState(
            translationX = obj.getDouble(KEY_TRANSLATION_X).toFloat(),
            translationY = obj.getDouble(KEY_TRANSLATION_Y).toFloat(),
            scaleX = obj.getDouble(KEY_SCALE_X).toFloat(),
            scaleY = obj.getDouble(KEY_SCALE_Y).toFloat(),
            rotation = obj.getDouble(KEY_ROTATION).toFloat(),
            pivotX = obj.getDouble(KEY_PIVOT_X).toFloat(),
            pivotY = obj.getDouble(KEY_PIVOT_Y).toFloat(),
            alpha = obj.getDouble(KEY_ALPHA).toFloat(),
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
