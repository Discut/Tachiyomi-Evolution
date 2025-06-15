package eu.kanade.tachiyomi.data.image.coil

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.model.IImageBo
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.model.UnionImageBO
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.model.SImage
import eu.kanade.tachiyomi.source.gallery.model.toSImage
import eu.kanade.tachiyomi.util.system.withIOContext
import okio.BufferedSource
import okio.buffer
import okio.source
import uy.kohesive.injekt.injectLazy
import java.io.InputStream

class ImageCacheFetcher(
    private val data: IImageBo,
    private val context: Context,
) : Fetcher {
    private val sourceManager: SourceManager by injectLazy()
    private val galleryManager: GalleryManager by injectLazy()

    override suspend fun fetch(): FetchResult {
        val source = sourceManager.getGallerySource(data.source)
        val sImage = when (data) {
            is ImageBO -> data.dbImage.toSImage()
            is UnionImageBO -> data.unions.first().dbImage.toSImage()
            else -> {
                error("Image not found")
            }
        }

        try {
            when (getResourceType(sImage)) {
                Type.URL -> {
                    val stream = withIOContext {
                        source?.getImageStream(sImage) ?: return@withIOContext null
                    }
                    if (stream == null) {
                        error("Image not found")
                    }
                    stream.apply {
                        stream.originStream.toDrawable(context)
                        return DrawableResult(
                            drawable = stream.originStream.toDrawable(context)
                                ?: error("Image not found"),
                            isSampled = false,
                            dataSource = DataSource.NETWORK,
                        )
                    }
                }

                Type.File -> {
                    val stream = withIOContext {
                        galleryManager.getThumbnail(data)
                    }
                    return SourceResult(
                        source = ImageSource(
                            source = stream.toBufferedSource(),
                            context = context,
                        ),
                        mimeType = "image/*",
                        dataSource = DataSource.DISK,
                    )
                }

                else -> error(
                    "Image type not supported: ${getResourceType(sImage)}",
                )
            }
        } catch (e: Exception) {
            error("Image not found")
        }
    }

    private fun getResourceType(sImage: SImage): Type? {
        val url = sImage.url
        return when {
            url.isEmpty() -> null
            url.startsWith("http") || url.startsWith("Custom-", true) -> Type.URL
            url.startsWith("/") || url.startsWith("file://") -> Type.File
            else -> null
        }
    }

    class Factory : Fetcher.Factory<ImageBO> {

        override fun create(data: ImageBO, options: Options, imageLoader: ImageLoader): Fetcher {
            return ImageCacheFetcher(data, options.context)
        }
    }

    private enum class Type {
        File, URL;
    }
}

fun InputStream.toDrawable(context: Context): Drawable? {
    return try {
        // 通过 BitmapFactory 解码流
        val bitmap = BitmapFactory.decodeStream(this)
        // 创建 BitmapDrawable 对象（需关联资源）
        BitmapDrawable(context.resources, bitmap)
    } catch (e: Exception) {
        null
    }
}

fun InputStream.toBufferedSource(): BufferedSource {
    return this.source().buffer()
}
