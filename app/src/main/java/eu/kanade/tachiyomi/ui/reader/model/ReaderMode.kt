package eu.kanade.tachiyomi.ui.reader.model

import android.os.Bundle

sealed class ReaderMode(val key: String) {
    /**
     * 漫画
     */
    data object MANGA : ReaderMode("manga")

    /**
     * 画廊
     */
    data object GALLERY : ReaderMode("gallery")

    companion object {
        fun valueOf(key: String): ReaderMode =
            when (key) {
                MANGA.key -> MANGA
                GALLERY.key -> GALLERY
                else -> MANGA
            }
    }
}

const val READER_MODE_KEY = "reader_mode"

fun Bundle?.getReaderMode(): ReaderMode {
    return if (this == null) {
        ReaderMode.MANGA
    } else {
        ReaderMode.valueOf(getString(READER_MODE_KEY) ?: ReaderMode.MANGA.key)
    }
}
