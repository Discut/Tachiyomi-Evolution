package eu.kanade.tachiyomi.ui.reader.sheet

data class AIPredictResult(
    val tagName: String,
    val score: Float,
    val isAdopted: Boolean = false,
)

enum class AIPredictState { IDLE, LOADING, RESULTS, EMPTY, ERROR }
