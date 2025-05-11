package eu.kanade.tachiyomi.data.image.coil

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import eu.kanade.tachiyomi.model.ImageBO
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.model.toSImage
import eu.kanade.tachiyomi.util.system.withIOContext
import uy.kohesive.injekt.injectLazy
import java.io.InputStream

class ImageCacheFetcher(
    private val data: ImageBO,
    private val context: Context,
) : Fetcher {
    private val sourceManager: SourceManager by injectLazy()

    override suspend fun fetch(): FetchResult {
        val stream = withIOContext {
            sourceManager.getGallerySource(data.source)?.getImageStream(data.dbImage.toSImage())
                ?: return@withIOContext null
        }

        /*        withDefContext {
                    BitmapFactory.decodeStream(stream)
                }*/

        if (stream == null) {
            error("Image not found")
        }

        stream.apply {
            return DrawableResult(
                drawable = stream.originStream.toDrawable(context)!!,
                isSampled = false,
                dataSource = DataSource.DISK,
            )
        }
    }

    class Factory : Fetcher.Factory<ImageBO> {

        override fun create(data: ImageBO, options: Options, imageLoader: ImageLoader): Fetcher {
            return ImageCacheFetcher(data, options.context)
        }
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
    } finally {
        this.close() // 必须关闭流防止泄漏[4,8](@ref)
    }
}
