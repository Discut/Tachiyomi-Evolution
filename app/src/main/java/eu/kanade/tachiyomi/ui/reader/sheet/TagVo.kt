package eu.kanade.tachiyomi.ui.reader.sheet

import eu.kanade.tachiyomi.model.TagBo

data class TagVo(
    val tagId: Long,
    val tagValue: String,
    val typeId: Long,
    val source: Long,
    var isSelected: Boolean,
)

fun TagBo.toTagVo(isSelected: Boolean) = TagVo(tagId, tagValue, typeId, source, isSelected)

fun TagVo.toTagBo() = TagBo(
    tagId = this@toTagBo.tagId,
    tagValue = this@toTagBo.tagValue,
    typeId = this@toTagBo.typeId,
    source = this@toTagBo.source,
)
