package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.gallery.GallerySource

/**
 * A factory for creating sources at runtime.
 */
interface SourceFactory {
    /**
     * Create a new copy of the sources
     * @return The created sources
     */
    fun createSources(): List<Source>

    /**
     * Create a new copy of the sources for the gallery
     * @return The created sources
     */
    fun createSourcesForGallery(): List<GallerySource>
}
