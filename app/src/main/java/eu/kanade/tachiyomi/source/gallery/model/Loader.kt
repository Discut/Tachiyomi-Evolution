package eu.kanade.tachiyomi.source.gallery.model

import eu.kanade.tachiyomi.network.ProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Transient

open class Loader : ProgressListener {
    sealed class State {
        object Queue : State()
        object LoadPage : State()
        object DownloadImage : State()
        object Ready : State()
        class Error(val error: Throwable, val msg: String) : State()
    }

    @Transient
    private val _statusFlow = MutableStateFlow<State>(State.Queue)

    @Transient
    val statusFlow = _statusFlow.asStateFlow()
    var status: State
        get() = _statusFlow.value
        set(value) {
            _statusFlow.value = value
        }

    @Transient
    private val _progressFlow = MutableStateFlow(0)

    @Transient
    val progressFlow = _progressFlow.asStateFlow()
    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }
}
