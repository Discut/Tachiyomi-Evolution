package eu.kanade.tachiyomi.ui.reader.slide.serializer

import android.content.Context
import eu.kanade.tachiyomi.data.orm.models.AnimationSequence
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * 动画序列导出/导入管理器
 *
 * 提供动画序列的导出和导入功能，用于备份和分享
 */
object AnimationSequenceExportManager {

    private const val EXPORT_DIR = "tachi"
    private const val ANIMATIONS_SUBDIR = "animations"
    private const val EXPORT_FILE_PREFIX = "export_"
    private const val EXPORT_FILE_EXTENSION = ".json"

    /**
     * 导出动画序列为 JSON 文件
     *
     * @param context 上下文
     * @param sequence 要导出的动画序列
     * @param customFileName 自定义文件名（可选），如果不提供则使用默认命名
     * @return 导出的文件
     * @throws ExportException 导出失败时抛出
     */
    fun export(
        context: Context,
        sequence: AnimationSequence,
        customFileName: String? = null,
    ): File {
        val exportDir = getExportDirectory(context, sequence.id)
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val fileName = customFileName ?: "$EXPORT_FILE_PREFIX${sequence.createdAt}$EXPORT_FILE_EXTENSION"
        val exportFile = File(exportDir, fileName)

        try {
            val exportJson = buildExportJson(sequence)
            exportFile.writeText(exportJson.toString())
            return exportFile
        } catch (e: JSONException) {
            throw ExportException("Failed to build export JSON", e)
        } catch (e: Exception) {
            throw ExportException("Failed to write export file", e)
        }
    }

    /**
     * 批量导出动画序列
     *
     * @param context 上下文
     * @param sequences 要导出的动画序列列表
     * @return 导出文件列表
     * @throws ExportException 导出失败时抛出
     */
    fun exportBatch(
        context: Context,
        sequences: List<AnimationSequence>,
    ): List<File> {
        val files = mutableListOf<File>()
        val errors = mutableListOf<Pair<AnimationSequence, Exception>>()

        sequences.forEach { sequence ->
            try {
                val file = export(context, sequence)
                files.add(file)
            } catch (e: Exception) {
                errors.add(sequence to e)
            }
        }

        if (errors.isNotEmpty()) {
            throw ExportException(
                "Failed to export ${errors.size} of ${sequences.size} sequences",
                errors.first().second,
            )
        }

        return files
    }

    /**
     * 从 JSON 文件导入动画序列
     *
     * @param jsonFile 导出的 JSON 文件
     * @return AnimationSequence 对象（不包含 id，需要在插入时生成）
     * @throws ImportException 导入失败时抛出
     */
    fun import(jsonFile: File): AnimationSequence {
        if (!jsonFile.exists()) {
            throw ImportException("File does not exist: ${jsonFile.path}")
        }

        return try {
            val json = JSONObject(jsonFile.readText())
            parseImportJson(json)
        } catch (e: JSONException) {
            throw ImportException("Invalid JSON format", e)
        } catch (e: Exception) {
            throw ImportException("Failed to parse import file", e)
        }
    }

    /**
     * 批量导入动画序列
     *
     * @param jsonFiles 导出的 JSON 文件列表
     * @return AnimationSequence 列表
     * @throws ImportException 导入失败时抛出
     */
    fun importBatch(jsonFiles: List<File>): List<AnimationSequence> {
        val sequences = mutableListOf<AnimationSequence>()
        val errors = mutableListOf<Pair<File, Exception>>()

        jsonFiles.forEach { file ->
            try {
                val sequence = import(file)
                sequences.add(sequence)
            } catch (e: Exception) {
                errors.add(file to e)
            }
        }

        if (errors.isNotEmpty()) {
            throw ImportException(
                "Failed to import ${errors.size} of ${jsonFiles.size} sequences",
                errors.first().second,
            )
        }

        return sequences
    }

    /**
     * 获取导出目录
     */
    private fun getExportDirectory(context: Context, sequenceId: Long): File {
        val baseDir = File(context.externalCacheDir, EXPORT_DIR)
        return File(baseDir, "$ANIMATIONS_SUBDIR/$sequenceId")
    }

    /**
     * 获取所有导出的动画序列文件
     */
    fun getExportFiles(context: Context): List<File> {
        val baseDir = File(context.externalCacheDir, "$EXPORT_DIR/$ANIMATIONS_SUBDIR")
        if (!baseDir.exists()) {
            return emptyList()
        }

        val files = mutableListOf<File>()
        baseDir.listFiles()?.forEach { sequenceDir ->
            sequenceDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(EXPORT_FILE_PREFIX) && file.name.endsWith(EXPORT_FILE_EXTENSION)) {
                    files.add(file)
                }
            }
        }

        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * 构建导出 JSON
     */
    private fun buildExportJson(sequence: AnimationSequence): JSONObject {
        return JSONObject().apply {
            put("id", sequence.id)
            put("imageId", sequence.imageId)
            put("name", sequence.name)
            put("duration", sequence.durationMs)
            put("data", sequence.data)
            put("exportedAt", System.currentTimeMillis())
            put("version", sequence.version)
            put("createdAt", sequence.createdAt)
            put("updatedAt", sequence.updatedAt)
        }
    }

    /**
     * 解析导入 JSON
     */
    private fun parseImportJson(json: JSONObject): AnimationSequence {
        return AnimationSequence(
            id = 0, // 插入时会自动生成
            imageId = json.getLong("imageId"),
            name = json.getString("name"),
            durationMs = json.getLong("duration"),
            data = json.getString("data"),
            version = json.getInt("version"),
            exported = false,
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        )
    }

    /**
     * 验证导出文件格式
     */
    fun validateExportFile(jsonFile: File): Boolean {
        if (!jsonFile.exists() || !jsonFile.isFile) {
            return false
        }

        return try {
            val json = JSONObject(jsonFile.readText())
            json.has("id") &&
                json.has("imageId") &&
                json.has("name") &&
                json.has("duration") &&
                json.has("data") &&
                json.has("version")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除指定序列的所有导出文件
     */
    fun deleteExportFiles(context: Context, sequenceId: Long): Boolean {
        val exportDir = getExportDirectory(context, sequenceId)
        return if (exportDir.exists()) {
            exportDir.deleteRecursively()
        } else {
            true
        }
    }

    /**
     * 导出异常
     */
    class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /**
     * 导入异常
     */
    class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
