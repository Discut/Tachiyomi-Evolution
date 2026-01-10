package eu.kanade.tachiyomi.util

import android.os.CountDownTimer
import android.os.SystemClock
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.LinearProgressIndicator
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy

class AutoPlayTimer(
    millisInFuture: Long,
    private val countDownInterval: Long,
    private val max: Int,
    private val progressBar: LinearProgressIndicator,
) :
    CountDownTimer(millisInFuture, countDownInterval) {

    private val preferences: PreferencesHelper by injectLazy()
    private var currentTime = 0
    var nextPageFun: (() -> Unit)? = null
    var doTick: DoTick = DoTick.PositivePlus
    var elapsedRealtimeSave = -1L
    override fun onTick(millisUntilFinished: Long) {
        currentTime += if (elapsedRealtimeSave != -1L) {
            // Use more accurate SystemClock.elapsedRealtime()
            (SystemClock.elapsedRealtime() - elapsedRealtimeSave).toInt()
        } else {
            countDownInterval.toInt()
        }
        elapsedRealtimeSave = SystemClock.elapsedRealtime()

        doTick.doTick(currentTime, max, progressBar)
        if (currentTime >= max) {
            currentTime = 0
            doTick = when (doTick) {
                DoTick.PositivePlus -> DoTick.NegativeMinus
                DoTick.NegativeMinus -> DoTick.NegativePlus
                DoTick.NegativePlus -> DoTick.PositiveMinus
                else -> DoTick.PositivePlus
            }
            nextPageFun?.invoke()
        }
    }

    override fun onFinish() {
        progressBar.progress = 0
        progressBar.isVisible = false
    }

    fun cancelTickAndProgress() {
        onFinish()
        cancel()
    }

    fun startTickAndProgress() {
        progressBar.isVisible = true && preferences.useAutoPlayProgress().get()
        start()
    }

    sealed class DoTick {
        open fun doTick(currentProgress: Int, max: Int, progressBar: LinearProgressIndicator) {}

        data object PositivePlus : DoTick() {
            override fun doTick(
                currentProgress: Int,
                max: Int,
                progressBar: LinearProgressIndicator,
            ) {
                progressBar.scaleX = 1f
                progressBar.progress = currentProgress
                progressBar.max = max
            }
        }

        data object NegativePlus : DoTick() {
            override fun doTick(
                currentProgress: Int,
                max: Int,
                progressBar: LinearProgressIndicator,
            ) {
                progressBar.scaleX = -1f
                progressBar.progress = currentProgress
                progressBar.max = max
            }
        }

        data object PositiveMinus : DoTick() {
            override fun doTick(
                currentProgress: Int,
                max: Int,
                progressBar: LinearProgressIndicator,
            ) {
                progressBar.scaleX = 1f
                progressBar.progress = max - currentProgress
                progressBar.max = max
            }
        }

        data object NegativeMinus : DoTick() {
            override fun doTick(
                currentProgress: Int,
                max: Int,
                progressBar: LinearProgressIndicator,
            ) {
                progressBar.scaleX = -1f
                progressBar.progress = max - currentProgress
                progressBar.max = max
            }
        }
    }
}
