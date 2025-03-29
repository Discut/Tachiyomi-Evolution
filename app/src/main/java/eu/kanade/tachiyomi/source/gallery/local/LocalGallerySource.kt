package eu.kanade.tachiyomi.source.gallery.local

import android.content.Context
import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.gallery.GallerySource
import eu.kanade.tachiyomi.source.gallery.model.Page
import eu.kanade.tachiyomi.source.gallery.model.SImage
import eu.kanade.tachiyomi.source.gallery.model.STag
import eu.kanade.tachiyomi.util.storage.DiskUtil
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 扫描本地的图库
 */
class LocalGallerySource(private val context: Context) : GallerySource, UnmeteredSource {
    val db: DatabaseHelper by injectLazy()

    override val id = ID
    override val name = context.getString(R.string.local_source)
    override val lang = "other"

    companion object {

        const val ID = 0L

        private fun getBaseDirectories(context: Context): List<File> {
            val basePath = listOf(
                context.getString(R.string.app_name) + File.separator + "gallery",
                "Pictures" + File.separator + "JHentaib",
            )
            return DiskUtil.getExternalStorages(context).map { c ->
                basePath.map { File(c.absolutePath, it) }
            }.flatten()
        }

        val SUPPORT_IMAGE = listOf("png", "jpg", "jpeg", "webp", "bmp")
    }

    override suspend fun getAllImages(): List<SImage> {
        val formater = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return getBaseDirectories(context).map { file ->
            file.walk().maxDepth(3)
                .filter { it.isFile && it.extension.lowercase() in SUPPORT_IMAGE }
                .toList()
        }.flatten().mapIndexed { index, file ->
            val modifiedTime = formater.format(Date(file.lastModified()))
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true // 只获取图片的宽高信息，不加载整个图片到内存中
            BitmapFactory.decodeFile(file.absolutePath, options)
            val width = options.outWidth
            val height = options.outHeight
            SImage(
                index = index,
                id = index.toLong(),
                createdAt = modifiedTime,
                modifiedAt = modifiedTime,
                fileSize = file.length(),
                name = file.nameWithoutExtension,
                url = file.absolutePath,
                width = width,
                height = height,
                source = ID,
            )
        }.filter { it.height > 0 && it.width > 0 }.toList()
    }

    override suspend fun collectImages(pageIndex: Int, pageSize: Int): Page<SImage> {
        val formater = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val base = getBaseDirectories(context).map { file ->
            file.walk().maxDepth(3)
                .filter { it.isFile && it.extension.lowercase() in SUPPORT_IMAGE }
                .toList()
        }.flatten().sortedBy {
            it.lastModified()
        }.asReversed()
        val total = base.count()
        val chunked = base.chunked(pageSize)
        if (pageIndex >= chunked.size) {
            return Page(
                index = 0,
                pageSize = pageSize,
                total = total,
                items = emptyList(),
            )
        }

        return Page(
            index = pageIndex,
            pageSize = pageSize,
            total = total,
            items = chunked[pageIndex].mapIndexed { index, file ->
                val modifiedTime = formater.format(Date(file.lastModified()))
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true // 只获取图片的宽高信息，不加载整个图片到内存中
                BitmapFactory.decodeFile(file.absolutePath, options)
                val width = options.outWidth
                val height = options.outHeight
                SImage(
                    index = index,
                    id = index.toLong(),
                    createdAt = modifiedTime,
                    modifiedAt = modifiedTime,
                    fileSize = file.length(),
                    name = file.nameWithoutExtension,
                    url = file.absolutePath,
                    width = width,
                    height = height,
                    source = ID,
                )
            }.toList(),
        )
    }

    override suspend fun getAllTags(): List<STag> {
        TODO("Not yet implemented")
    }

    override suspend fun collectTags(pageIndex: Int, pageSize: Int): Page<STag> {
        TODO("Not yet implemented")
    }

    override suspend fun collectImagesByTag(
        tag: STag,
        pageIndex: Int,
        pageSize: Int,
    ): Page<SImage> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllImagesByTag(tag: STag): List<SImage> {
        TODO("Not yet implemented")
    }
}
