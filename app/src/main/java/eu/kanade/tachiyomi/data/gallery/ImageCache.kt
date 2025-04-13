package eu.kanade.tachiyomi.data.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.withIOContext
import java.io.File
import java.io.FileDescriptor
import java.io.InputStream

class ImageCache(
    context: Context,
) {
    private val cache: DiskLruCache

    fun getAsStream(key: String): InputStream {
        return cache.get(key)?.getInputStream(DISK_IMAGE_CACHE_INDEX)
            ?: throw Exception("key not found")
    }

    fun isExist(key: String): Boolean {
        return cache.get(key) != null
    }

    suspend fun put(key: String, value: InputStream) = withIOContext {
        cache.edit(key)?.apply {
            try {
                newOutputStream(DISK_IMAGE_CACHE_INDEX).use { it.write(value.readBytes()) }
                commit()
            } catch (e: Exception) {
                abort()
            } finally {
                cache.flush()
                value.close()
            }
        }
    }

    init {
        val externalStorages = DiskUtil.getExternalStorages(context)
        val diskCacheDir = if (externalStorages.isEmpty()) {
            File(context.cacheDir, DISK_IMAGE_CACHE_NAME) // context.cacheDir
        } else {
            File(externalStorages.first().absolutePath, DISK_IMAGE_CACHE_NAME)
        }

        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
        cache = DiskLruCache.open(
            diskCacheDir,
            DISK_IMAGE_CACHE_VERSION,
            1,
            DISK_IMAGE_CACHE_SIZE.toLong(),
        )
    }

    private fun decodeSampledBitmapFromFileDescriptor(
        fileDescriptor: FileDescriptor?,
        scale: Double = 1.0,
    ): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        // 获取图片的原始宽高信息
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
        val reqWidth = (options.outWidth * scale).toInt()
        val reqHeight = (options.outHeight * scale).toInt()
        return decodeSampledBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight)
    }

    private fun decodeSampledBitmapFromFileDescriptor(
        fileDescriptor: FileDescriptor?,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        // 获取图片的原始宽高信息
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)

        // 计算采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // 根据计算出的采样率进行实际解码
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    companion object {
        private const val DISK_IMAGE_CACHE_SIZE = 512 * 1024 * 1024 // 512MB
        private const val DISK_IMAGE_CACHE_VERSION = 1
        private const val DISK_IMAGE_CACHE_NAME = "gallery_cache"
        private const val DISK_IMAGE_CACHE_INDEX = 0
    }
}
