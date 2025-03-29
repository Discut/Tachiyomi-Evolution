package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.model.GalleryBo
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.local.LocalGallerySource
import eu.kanade.tachiyomi.source.gallery.online.IHttpGallerySource
import eu.kanade.tachiyomi.source.model.Page.State
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.toSImage
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

class ImageLoader(private val galleryBo: GalleryBo) : PageLoader() {
    private val chapterCache: ChapterCache = Injekt.get()
    private val preferences: PreferencesHelper = Injekt.get()
    private val sourceManager: SourceManager = Injekt.get()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * A queue used to manage requests one by one while allowing priorities.
     */
    private val queue =
        PriorityBlockingQueue<PriorityPage>()

    private val preloadSize = preferences.preloadSize().get()

    init {
        scope.launchIO {
            flow {
                while (true) {
                    emit(runInterruptible { queue.take() }.page)
                }
            }
                .filter { it.status == State.QUEUE }
                .collect {
                    _loadPage(it)
                }
        }
    }

    override suspend fun getPages(): List<ReaderPage> {
        return galleryBo.images.mapIndexed { index, imageBO ->
            when (imageBO.source) {
                LocalGallerySource.ID -> {
                    val file = File(imageBO.url)
                    var streamFun: (() -> InputStream)? = null
                    if (file.exists()) {
                        streamFun = { FileInputStream(file) }
                    }
                    ReaderPage(
                        index = index,
                        imageUrl = imageBO.url,
                        url = imageBO.url,
                        stream = streamFun,
                    ).apply {
                        status = if (streamFun == null) State.ERROR else State.READY
                        sourceId = LocalGallerySource.ID
                        id = imageBO.id
                    }
                }

                else -> {
                    ReaderPage(
                        index = index,
                        imageUrl = imageBO.url,
                        url = imageBO.url,
                    ).apply {
                        sourceId = imageBO.source
                        id = imageBO.id
                    }
                }
            }
        }
    }

    override suspend fun loadPage(page: ReaderPage) {
        withIOContext {
            if (page.status == State.READY) {
                return@withIOContext
            }
            val imageUrl = page.imageUrl

            // Check if the image has been deleted
            if (page.status == State.READY && imageUrl != null && !chapterCache.isImageInCache(
                    imageUrl,
                )
            ) {
                page.status = State.QUEUE
            }

            // Automatically retry failed pages when subscribed to this page
            if (page.status == State.ERROR) {
                page.status = State.QUEUE
            }

            val queuedPages = mutableListOf<PriorityPage>()
            if (page.status == State.QUEUE) {
                queuedPages += PriorityPage(page, 1).also { queue.offer(it) }
            }
            queuedPages += preloadNextPages(page, preloadSize)

            suspendCancellableCoroutine<Nothing> { continuation ->
                continuation.invokeOnCancellation {
                    queuedPages.forEach {
                        if (it.page.status == State.QUEUE) {
                            queue.remove(it)
                        }
                    }
                }
            }
        }
    }

    /**
     * Data class used to keep ordering of pages in order to maintain priority.
     */
    private class PriorityPage(
        val page: ReaderPage,
        val priority: Int,
    ) : Comparable<PriorityPage> {
        companion object {
            private val idGenerator = AtomicInteger()
        }

        private val identifier = idGenerator.incrementAndGet()

        override fun compareTo(other: PriorityPage): Int {
            val p = other.priority.compareTo(priority)
            return if (p != 0) p else identifier.compareTo(other.identifier)
        }
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary.
     * Downloaded images are stored in the chapter cache.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private suspend fun _loadPage(page: ReaderPage) {
        try {
            val source =
                sourceManager.getGallerySource(page.sourceId) ?: throw Exception("Source not found")
            if (source !is IHttpGallerySource) {
                throw Exception("Source not supported")
            }
            if (page.imageUrl.isNullOrEmpty()) {
                page.status = State.LOAD_PAGE
                page.imageUrl = source.getImageUrl(page.toSImage())
            }
            val imageUrl = page.imageUrl!!

            if (!chapterCache.isImageInCache(imageUrl)) {
                page.status = State.DOWNLOAD_IMAGE
                val imageResponse = source.getImage(page.toSImage())
                chapterCache.putImageToCache(imageUrl, imageResponse)
            }

            page.stream = { chapterCache.getImageFile(imageUrl).inputStream() }
            page.status = State.READY
        } catch (e: Throwable) {
            page.status = State.ERROR
            if (e is CancellationException) {
                throw e
            }
        }
    }

    /**
     * Preloads the given [amount] of pages after the [currentPage] with a lower priority.
     * @return a list of [PriorityPage] that were added to the [queue]
     */
    private fun preloadNextPages(
        currentPage: ReaderPage,
        amount: Int,
    ): List<PriorityPage> {
        val pageIndex = currentPage.index
        val pages = currentPage.chapter.pages ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        return pages
            .subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
            .mapNotNull {
                if (it.status == State.QUEUE) {
                    PriorityPage(it, 0)
                        .apply { queue.offer(this) }
                } else {
                    null
                }
            }
    }
}
