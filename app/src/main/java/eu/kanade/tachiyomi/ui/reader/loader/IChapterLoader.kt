package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter

interface IChapterLoader {
    suspend fun loadChapter(chapter: ReaderChapter)
}
