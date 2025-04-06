package eu.kanade.tachiyomi.error

class TipsException(
    override val message: String,
    val messageResId: Int? = null,
    val isNeedLog: Boolean = false,
    val isHandled: Boolean = false,
) : Exception(message)
