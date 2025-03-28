package eu.kanade.tachiyomi.source.gallery.model

class Page<T>(
    val index: Int,
    val pageSize: Int,
    val total: Int,
    val items: List<T>,
) {
    fun currentPage() = index + 1
    fun hasNextPage() = currentPage() < total

    fun hasPreviousPage() = index > 0

    fun nextPage() = Page(index + pageSize, pageSize, total, items)

    fun previousPage() = Page(index - pageSize, pageSize, total, items)

    fun totalPages() = (total / pageSize) + if (total % pageSize == 0) 0 else 1
}
