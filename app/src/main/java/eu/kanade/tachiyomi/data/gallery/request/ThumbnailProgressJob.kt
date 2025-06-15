package eu.kanade.tachiyomi.data.gallery.request

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 将图片处理成缩略图，后台任务
 */
class ThumbnailProgressJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }
}
