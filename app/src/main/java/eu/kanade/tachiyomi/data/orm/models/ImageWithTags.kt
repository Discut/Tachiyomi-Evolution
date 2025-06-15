package eu.kanade.tachiyomi.data.orm.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import eu.kanade.tachiyomi.data.database.tables.ImageTable
import eu.kanade.tachiyomi.data.database.tables.TagTable

data class ImageWithTags(

    @Embedded
    val image: DBImage,

    @Relation(
        parentColumn = ImageTable.ID,
        entityColumn = TagTable.TAG_ID,
        associateBy = Junction(DBImageAndTag::class),
    )
    val tags: List<DBTag>?,
)
