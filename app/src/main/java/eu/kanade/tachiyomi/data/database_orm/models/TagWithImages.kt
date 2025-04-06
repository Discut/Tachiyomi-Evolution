package eu.kanade.tachiyomi.data.database_orm.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import eu.kanade.tachiyomi.data.database.tables.ImageTable
import eu.kanade.tachiyomi.data.database.tables.TagTable

data class TagWithImages(
    @Embedded
    val tag: DBTag,
    @Relation(
        parentColumn = TagTable.TAG_ID,
        entityColumn = ImageTable.ID,
        associateBy = Junction(DBImageAndTag::class),
    )
    val images: List<DBImage>?,
)
