package eu.kanade.tachiyomi.data.tagger

import android.content.Context

data class ModelTag(
    val id: Int,
    val name: String,
    val nameCn: String,
    val category: Int, // 0=general, 1=artist, 3=copyright, 4=character, 5=meta
)

private fun loadChineseMap(context: Context): Map<String, String> {
    return context.assets.open("tags_info.zh_CN.csv").bufferedReader().use { reader ->
        reader.lineSequence()
            .mapNotNull { line ->
                val commaIdx = line.indexOf(',')
                if (commaIdx > 0) {
                    line.substring(0, commaIdx) to cleanCn(line.substring(commaIdx + 1))
                } else {
                    null
                }
            }
            .toMap()
    }
}

// 清洗中文值：去掉英文单词、表情符号，去重，只保留 CJK 内容
private fun cleanCn(raw: String): String {
    return raw.split(" ")
        .filter { token -> token.any { isCjk(it) } }
        .distinct()
        .joinToString(" ")
}

private fun isCjk(c: Char): Boolean = c in '\u4e00'..'\u9fff'

fun loadTags(context: Context): List<ModelTag> {
    val cnMap = loadChineseMap(context)
    return context.assets.open("tags_info.csv").bufferedReader().useLines { lines ->
        lines.drop(1) // 跳过表头
            .map { line ->
                val parts = line.split(",")
                val englishName = parts[1]
                ModelTag(
                    id = parts[0].toInt(),
                    name = parts[1],
                    nameCn = cnMap[englishName] ?: englishName,
                    category = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                )
            }
            .toList()
    }
}
