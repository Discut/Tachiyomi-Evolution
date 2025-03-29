package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.model.GalleryBo
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

sealed class ReaderChapter {
    val stateFlow = MutableStateFlow<State>(State.Wait)
    var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    val pages: List<ReaderPage>?
        get() = (state as? State.Loaded)?.pages

    var pageLoader: PageLoader? = null

    var requestedPage: Int = 0

    protected var references = 0

    open var chapterId = 0L

    open var url = ""

    open fun ref() {
    }

    open fun unref() {
    }

    sealed class State {
        object Wait : State()
        object Loading : State()
        class Error(val error: Throwable) : State()
        class Loaded(val pages: List<ReaderPage>) : State()
    }

    data class MangaChapter(val chapter: Chapter) : ReaderChapter() {
        override var chapterId: Long = chapter.id ?: -1
        override var url = chapter.url

        override fun ref() {
            references++
        }

        override fun unref() {
            references--
            if (references == 0) {
                if (pageLoader != null) {
                    Timber.d("Recycling chapter ${chapter.name}")
                }
                pageLoader?.recycle()
                pageLoader = null
                state = State.Wait
            }
        }
    }

    data class Gallery(val galleryBo: GalleryBo) : ReaderChapter() {

        override var chapterId: Long = galleryBo.id ?: -1

        override var url = "nothing, it's a gallery"

        override fun ref() {
            references++
        }

        override fun unref() {
            references--
            if (references == 0) {
                if (pageLoader != null) {
                    Timber.d("Recycling chapter $galleryBo")
                }
                pageLoader?.recycle()
                pageLoader = null
                state = State.Wait
            }
        }
    }
}
