package eu.kanade.tachiyomi.source.gallery.local

import android.content.Context
import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.gallery.ImageCache
import eu.kanade.tachiyomi.data.gallery.compressImageStream
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.gallery.GallerySource
import eu.kanade.tachiyomi.source.gallery.model.ImageStream
import eu.kanade.tachiyomi.source.gallery.model.Page
import eu.kanade.tachiyomi.source.gallery.model.SImage
import eu.kanade.tachiyomi.source.gallery.model.STag
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.withDefContext
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.toLongBySHA256
import uy.kohesive.injekt.injectLazy
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 扫描本地的图库
 */
class LocalGallerySource(private val context: Context) : GallerySource, UnmeteredSource {
    val db: DatabaseHelper by injectLazy()
    val cache: ImageCache by injectLazy()
    val preference: PreferencesHelper by injectLazy()

    override val id = ID
    override val name = context.getString(R.string.local_source)
    override val lang = "other"

    override suspend fun getImageStream(image: SImage): ImageStream? =
        try {
            with(File(image.url)) {
                if (isFile && ImageUtil.isImage(name)) {
                    val (width, height) = if (image.width > 0 && image.height > 0) {
                        image.width to image.height
                    } else {
                        getImageDimensions() ?: return null
                    }
                    ImageStream(
                        width = width,
                        height = height,
                        originStream = inputStream(),
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }

    override suspend fun getThumbnailImageStream(image: SImage): ImageStream? = withIOContext {
        val imageStream = if (cache.isExist(image.id.toString())) {
            cache.getAsStream(image.id.toString())
        } else {
            val imageStream = getImageStream(image) ?: return@withIOContext null
            val compressImage = withDefContext {
                compressImageStream(
                    imageStream.originStream,
                    imageStream.width / 5,
                    imageStream.height / 5,
                )
            }
            cache.put(
                image.id.toString(),
                ByteArrayInputStream(compressImage),
            )
            cache.getAsStream(image.id.toString())
        }
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val (width, height) = try {
            withDefContext {
                BitmapFactory.decodeStream(imageStream, null, options)
                options.outWidth to options.outHeight
            }
        } catch (e: Exception) {
            return@withIOContext null
        }
        ImageStream(
            width = width,
            height = height,
            originStream = imageStream,
        )
    }

    companion object {

        const val ID = 0L

        private fun getBaseDirectories(
            context: Context,
            preference: PreferencesHelper,
        ): List<File> {
            val basePath = listOf(
                context.getString(R.string.app_name) + File.separator + "gallery",
            )

            val excludedPaths = preference.getGalleryScanExcludes().get()
            val includedPaths = preference.getGalleryScanIncludes().get()
                .filterNot(excludedPaths::contains)
                .takeIf { it.isNotEmpty() }
                ?: basePath

            return sequence {
                yield(File("/")) // 根目录
                yieldAll(DiskUtil.getExternalStorages(context).map { it.absoluteFile })
            }.flatMap { storageRoot ->
                includedPaths.map { subDir ->
                    File(storageRoot, subDir).apply {
                        mkdirs() // 确保目录存在
                    }
                }
            }.toList()/*
            return (listOf(File(File.separator)) + DiskUtil.getExternalStorages(context).toList())
                .map { c ->
                    includedPaths.map { File(c.absolutePath, it) }
            }.flatten()*/
        }

        val SUPPORT_IMAGE = listOf("png", "jpg", "jpeg", "webp", "bmp")
    }

    override suspend fun getAllImages(): List<SImage> {
        val formater = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return getBaseDirectories(context, preference).map { file ->
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
                id = file.absolutePath.toLongBySHA256(),
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

        val base = getBaseDirectories(context, preference).map { file ->
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

private fun File.getImageDimensions(): Pair<Int, Int>? {
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
