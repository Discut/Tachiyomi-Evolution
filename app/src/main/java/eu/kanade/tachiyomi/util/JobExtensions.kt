package eu.kanade.tachiyomi.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

fun Job?.cancelOldJob() {
    this?.cancel(CancellationException("取消旧任务"))
}
