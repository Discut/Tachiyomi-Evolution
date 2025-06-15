package eu.kanade.tachiyomi.util

import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.UnionImageBO
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.model.ImageStream
import eu.kanade.tachiyomi.source.gallery.model.SImage
import eu.kanade.tachiyomi.source.gallery.model.toSImage
import uy.kohesive.injekt.injectLazy
import java.io.File

suspend fun SImage.getImageStream(): ImageStream? {
    val sourceManager: SourceManager by injectLazy()
    val source = sourceManager.getGallerySource(source)
    return source?.getImageStream(this)
}

fun File.getImageDimensions(): Pair<Int, Int>? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    return try {
        BitmapFactory.decodeFile(this.absolutePath, options)
        options.outWidth to options.outHeight
    } catch (e: Exception) {
        null
    }
}

fun IImageBo.dbImage() = when (this) {
    is ImageBO -> dbImage
    is UnionImageBO -> unions.first().dbImage
    else -> error("Image not found")
}

fun IImageBo.toSImage() = dbImage().toSImage()
