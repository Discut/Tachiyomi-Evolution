package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.model.GalleryBo
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.system.withIOContext
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

class GalleryLoader(
    private val context: Context,
    private val gallery: GalleryBo,
) : IChapterLoader {

    private val sourceManager: SourceManager by injectLazy()

    override suspend fun loadChapter(chapter: ReaderChapter) {
        if (chapterIsReady(chapter) || chapter !is ReaderChapter.Gallery) {
            return
        }

        chapter.state = ReaderChapter.State.Loading
        withIOContext {
            Timber.d("Loading images of gallery for ${chapter.galleryBo.title}")
            try {
                val loader = ImageLoader(gallery)
                chapter.pageLoader = loader

                val pages = loader.getPages()
                    .onEach { it.chapter = chapter }

                if (pages.isEmpty()) {
                    throw Exception(context.getString(R.string.no_pages_found))
                }

                // If the chapter is partially read, set the starting page to the last the user read
                // otherwise use the requested page.
/*                if (!chapter.chapter.read) {
                    chapter.requestedPage = chapter.chapter.last_page_read
                }*/

                chapter.state = ReaderChapter.State.Loaded(pages)
            } catch (e: Throwable) {
                chapter.state = ReaderChapter.State.Error(e)
                throw e
            }
        }
    }

    private fun chapterIsReady(chapter: ReaderChapter): Boolean {
        return chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null
    }
}
