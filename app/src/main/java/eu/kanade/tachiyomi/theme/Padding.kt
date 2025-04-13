package eu.kanade.tachiyomi.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

object Padding {
    val Small = 4.dp
    val Default = 8.dp
    val Medium = 12.dp
    val Normal = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val ExtraBiggerLarge = 48.dp
}

val MaterialTheme.padding: Padding
    get() = Padding
