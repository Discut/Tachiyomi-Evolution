package eu.kanade.tachiyomi.util

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.toDate(pattern: String = "yyyy-MM-dd HH:mm:ss"): Date {
    return try {
        SimpleDateFormat(pattern, Locale.getDefault()).parse(this)
            ?: throw Exception("Date is null")
    } catch (e: Exception) {
        Date()
    }
}

fun String.toLongBySHA256(): Long {
    val sha256 = MessageDigest.getInstance("SHA-256")
    val bytes = sha256.digest(toByteArray())
    return (bytes[0].toLong() and 0xFFL) shl 56 or
        (bytes[1].toLong() and 0xFFL) shl 48 or
        (bytes[2].toLong() and 0xFFL) shl 40 or
        (bytes[3].toLong() and 0xFFL) shl 32 or
        (bytes[4].toLong() and 0xFFL) shl 24 or
        (bytes[5].toLong() and 0xFFL) shl 16 or
        (bytes[6].toLong() and 0xFFL) shl 8 or
        (bytes[7].toLong() and 0xFFL)
}
