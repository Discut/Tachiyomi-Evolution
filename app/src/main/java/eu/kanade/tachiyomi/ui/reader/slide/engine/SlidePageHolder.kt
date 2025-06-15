package eu.kanade.tachiyomi.ui.reader.slide.engine

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPageHolder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer

@SuppressLint("ViewConstructor")
class SlidePageHolder(
    viewer: PagerViewer,
    page: ReaderPage,
    private var extraPage: ReaderPage? = null,
) : PagerPageHolder(viewer, page, extraPage) {
}
