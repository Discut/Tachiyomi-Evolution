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
 *
 * Version 2 (新格式 - 归一化值):
 * {
 *   "version": 2,
 *   "duration": 10000,
 *   "imageWidth": 2000,
 *   "imageHeight": 3000,
 *   "keyFrames": [
 *     {
 *       "timeMs": 0,
 *       "state": {
 *         "normalizedTranslationX": 0,
 *         "normalizedTranslationY": 0,
 *         "normalizedScale": 1,
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
 * 支持版本 1（旧格式）和版本 2（归一化格式）
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

    // Version 2 字段（归一化格式）
    private const val KEY_NORMALIZED_TRANSLATION_X = "normalizedTranslationX"
    private const val KEY_NORMALIZED_TRANSLATION_Y = "normalizedTranslationY"
    private const val KEY_NORMALIZED_SCALE = "normalizedScale"

    private const val KEY_ROTATION = "rotation"
    private const val KEY_PIVOT_X = "pivotX"
    private const val KEY_PIVOT_Y = "pivotY"
    private const val KEY_ALPHA = "alpha"

    private const val CURRENT_VERSION = 2

    /**
     * 将 SlideAnimationPath 序列化为 JSON 字符串（版本 2）
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

                put(KEY_KEY_FRAMES, serializeKeyFramesV2(path.keyFrames))
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
                2 -> deserializeFromV2(jsonObject)
                else -> throw IllegalArgumentException("Unsupported version: $version")
            }
        } catch (e: JSONException) {
            throw SerializationException("Failed to deserialize animation path", e)
        } catch (e: IllegalArgumentException) {
            throw SerializationException("Invalid animation data", e)
        }
    }

    /**
     * 从版本 1 格式迁移（旧格式 → 新格式）
     * 旧数据使用绝对值，迁移时保持原样（需要重新编辑才能获得跨设备兼容性）
     */
    private fun migrateFromV1(jsonObject: JSONObject): SlideAnimationPath {
        Timber.d("Migrating animation sequence from version 1 to version 2")
        val duration = jsonObject.getLong(KEY_DURATION)
        val keyFramesArray = jsonObject.getJSONArray(KEY_KEY_FRAMES)

        val keyFrames = mutableListOf<KeyFrame>()
        for (i in 0 until keyFramesArray.length()) {
            val obj = keyFramesArray.getJSONObject(i)
            val timeMs = obj.getLong(KEY_TIME_MS)
            val stateObj = obj.getJSONObject(KEY_STATE)

            // 读取旧格式数据
            val translationX = stateObj.getDouble(KEY_TRANSLATION_X).toFloat()
            val translationY = stateObj.getDouble(KEY_TRANSLATION_Y).toFloat()
            val scaleX = stateObj.getDouble(KEY_SCALE_X).toFloat()
            val scaleY = stateObj.getDouble(KEY_SCALE_Y).toFloat()
            val rotation = stateObj.getDouble(KEY_ROTATION).toFloat()
            val pivotX = stateObj.getDouble(KEY_PIVOT_X).toFloat()
            val pivotY = stateObj.getDouble(KEY_PIVOT_Y).toFloat()
            val alpha = stateObj.getDouble(KEY_ALPHA).toFloat()

            // 使用 fromLegacy 创建新格式（保持旧值作为归一化值）
            // 注意：迁移后的数据在不同设备上可能显示不一致
            // 建议用户重新编辑关键帧以获得正确的跨设备兼容性
            val state = ImageViewState.fromLegacy(
                translationX = translationX,
                translationY = translationY,
                scaleX = scaleX,
                scaleY = scaleY,
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
     * 从版本 2 格式反序列化（归一化格式）
     */
    private fun deserializeFromV2(jsonObject: JSONObject): SlideAnimationPath {
        val duration = jsonObject.getLong(KEY_DURATION)
        val imageWidth = jsonObject.optInt(KEY_IMAGE_WIDTH, 0)
        val imageHeight = jsonObject.optInt(KEY_IMAGE_HEIGHT, 0)
        val keyFramesArray = jsonObject.getJSONArray(KEY_KEY_FRAMES)

        val keyFrames = deserializeKeyFramesV2(keyFramesArray, imageWidth, imageHeight)

        return SlideAnimationPath(
            durationMs = duration,
            keyFrames = keyFrames,
        )
    }

    /**
     * 序列化关键帧列表为 JSONArray（版本 2）
     */
    private fun serializeKeyFramesV2(keyFrames: List<KeyFrame>): JSONArray {
        return JSONArray().apply {
            keyFrames.forEach { frame ->
                put(
                    JSONObject().apply {
                        put(KEY_TIME_MS, frame.timeMs)
                        put(KEY_STATE, serializeImageViewStateV2(frame.state))
                    },
                )
            }
        }
    }

    /**
     * 从 JSONArray 反序列化关键帧列表（版本 2）
     */
    private fun deserializeKeyFramesV2(
        array: JSONArray,
        imageWidth: Int,
        imageHeight: Int,
    ): List<KeyFrame> {
        val keyFrames = mutableListOf<KeyFrame>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val timeMs = obj.getLong(KEY_TIME_MS)
            val stateObj = obj.getJSONObject(KEY_STATE)

            val state = deserializeImageViewStateV2(stateObj, imageWidth, imageHeight)

            keyFrames.add(KeyFrame(timeMs, state))
        }
        return keyFrames
    }

    /**
     * 序列化 ImageViewState 为 JSONObject（版本 2）
     */
    private fun serializeImageViewStateV2(state: ImageViewState): JSONObject {
        return JSONObject().apply {
            put(KEY_NORMALIZED_TRANSLATION_X, state.normalizedTranslationX)
            put(KEY_NORMALIZED_TRANSLATION_Y, state.normalizedTranslationY)
            put(KEY_NORMALIZED_SCALE, state.normalizedScale)
            put(KEY_ROTATION, state.rotation)
            put(KEY_PIVOT_X, state.pivotX)
            put(KEY_PIVOT_Y, state.pivotY)
            put(KEY_ALPHA, state.alpha)
        }
    }

    /**
     * 从 JSONObject 反序列化 ImageViewState（版本 2）
     */
    private fun deserializeImageViewStateV2(
        obj: JSONObject,
        imageWidth: Int,
        imageHeight: Int,
    ): ImageViewState {
        return ImageViewState(
            normalizedTranslationX = obj.getDouble(KEY_NORMALIZED_TRANSLATION_X).toFloat(),
            normalizedTranslationY = obj.getDouble(KEY_NORMALIZED_TRANSLATION_Y).toFloat(),
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
