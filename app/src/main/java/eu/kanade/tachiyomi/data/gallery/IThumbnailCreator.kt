package eu.kanade.tachiyomi.data.gallery

import eu.kanade.tachiyomi.model.IImageBo
import java.io.InputStream

interface IThumbnailCreator {

    fun createThumbnail(image: IImageBo)

    suspend fun getThumbnail(image: IImageBo): InputStream

    fun existsThumbnail(image: IImageBo): Boolean

    fun deleteThumbnail(image: IImageBo)

    fun boot()
}
