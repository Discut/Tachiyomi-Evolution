package eu.kanade.tachiyomi.theme

import androidx.compose.material3.MaterialTheme

object Alpha {
    val Disabled = 0.38f
    const val Lowest = 0.6f
    const val Normal = 0.7f
    val High = 0.87f
    const val Highest = 1f
}

val MaterialTheme.alpha: Alpha
    get() = Alpha
