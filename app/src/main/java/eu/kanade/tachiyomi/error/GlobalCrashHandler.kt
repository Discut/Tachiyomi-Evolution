package eu.kanade.tachiyomi.error

import android.content.Context
import eu.kanade.tachiyomi.util.system.toast
import timber.log.Timber

/**
 * 全局错误捕获
 */
class GlobalCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler(),
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 记录崩溃日志
        Timber.e(throwable)

        when (throwable) {
            is TipsException -> {
                if (throwable.isHandled) {
                    return
                }
                throwable.messageResId?.let {
                    context.toast(it)
                } ?: context.toast(throwable.message)

                if (throwable.isNeedLog) {
                    Timber.w(throwable)
                }
            }

            else -> {
                // 调用系统默认处理（触发崩溃弹窗）
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
