package eu.kanade.tachiyomi.ui.gallery.tags

import eu.kanade.tachiyomi.model.IImageBo

data class TagVo(
    val tagId: Long,
    val name: String,
    val images: List<IImageBo> = emptyList(),
    var cover: IImageBo? = null,
)
