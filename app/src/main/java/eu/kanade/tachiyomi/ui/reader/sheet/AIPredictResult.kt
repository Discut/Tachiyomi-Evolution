package eu.kanade.tachiyomi.ui.reader.sheet

data class AIPredictResult(
    val tagName: String,
    val score: Float,
    val isAdopted: Boolean = false,
    val isFiltered: Boolean = false,
)

enum class AIPredictState { IDLE, LOADING, REFRESHING, RESULTS, EMPTY, ERROR }
