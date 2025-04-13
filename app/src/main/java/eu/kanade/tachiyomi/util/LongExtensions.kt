package eu.kanade.tachiyomi.util

fun generateTimestampBasedID(): Long {
    val timestamp = System.currentTimeMillis() // 13位时间戳
    val random = (1000..9999).random() // 4位随机数
    return "$timestamp$random".toLong() // 组合成17位数字
}
