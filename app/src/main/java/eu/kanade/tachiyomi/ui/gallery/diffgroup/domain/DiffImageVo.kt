package eu.kanade.tachiyomi.ui.gallery.diffgroup.domain

import eu.kanade.tachiyomi.model.ImageBO
import java.math.BigDecimal

data class DiffImageVo(
    val diffGroupId: String,
    val image: ImageBO,
    val order: BigDecimal = image.order,
)
