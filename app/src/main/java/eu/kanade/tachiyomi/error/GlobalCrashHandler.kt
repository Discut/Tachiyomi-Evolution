package eu.kanade.tachiyomi.error

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.Application
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.util.system.toast
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * 全局错误捕获
 */
class GlobalCrashHandler(
    private val context: Application,
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

                showErrorToast(throwable)

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

    private fun showErrorToast(throwable: TipsException) {
        (context as? App)?.apply { // 自定义 Application 类
            if (isAppInForeground(context)) {
                ActivityHolder.currentActivity?.get()?.apply {
                    runOnUiThread {
                        throwable.messageResId?.let {
                            context.toast(it)
                        } ?: context.toast(throwable.message)
                    }
                }
            } else {
                // 发送系统通知或记录到文件
            }
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        return runningProcesses.any {
            it.processName == context.packageName && it.importance == IMPORTANCE_FOREGROUND
        }
    }
}

object ActivityHolder {
    @Volatile var currentActivity: WeakReference<Activity>? = null
}
