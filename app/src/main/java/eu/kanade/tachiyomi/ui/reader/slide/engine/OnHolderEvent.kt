package eu.kanade.tachiyomi.ui.reader.slide.engine

import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPageHolder

typealias OnHolderLoaded = (PagerPageHolder) -> Unit

interface OnHolderEvent {
    fun onLoaded(holder: PagerPageHolder)
}
