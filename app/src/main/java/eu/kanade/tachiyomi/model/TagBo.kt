package eu.kanade.tachiyomi.model

import eu.kanade.tachiyomi.data.orm.models.DBTag

data class TagBo(
    val tagId: Long,
    val tagValue: String,
    val typeId: Long = 0,
    val source: Long = 0,
)

fun TagBo.toDBTag() =
    DBTag(
        tagId = this@toDBTag.tagId,
        typeId = this@toDBTag.typeId,
        tagValue = this@toDBTag.tagValue,
        source = this@toDBTag.source,
    )

fun DBTag.toTagBo() =
    TagBo(
        tagId = this@toTagBo.tagId,
        tagValue = this@toTagBo.tagValue,
        typeId = this@toTagBo.typeId,
        source = this@toTagBo.source,
    )
