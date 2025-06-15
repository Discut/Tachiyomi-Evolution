package eu.kanade.tachiyomi.data.orm.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import eu.kanade.tachiyomi.data.database.tables.ImageTable
import eu.kanade.tachiyomi.data.database.tables.ImageTagTable
import eu.kanade.tachiyomi.data.database.tables.TagTable
import eu.kanade.tachiyomi.data.orm.models.DBImageAndTag
import eu.kanade.tachiyomi.data.orm.models.DBImageTagRelationForBackup

@Dao
interface ImageAndTagDao : BaseDao<DBImageAndTag> {

    @Query(
        "SELECT * FROM ${ImageTagTable.TABLE}",
    )
    fun getAll(): List<DBImageAndTag>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: DBImageAndTag)

    @Query(
        """
SELECT 
    i.${ImageTable.FILE_PATH},
    GROUP_CONCAT(COALESCE(t.${TagTable.TAG_VALUE}, '')) AS tag_value
FROM ${ImageTable.TABLE} i
LEFT JOIN ${ImageTagTable.TABLE} it ON i.${ImageTable.ID} = it.${ImageTagTable.IMAGE_ID}
LEFT JOIN ${TagTable.TABLE} t ON it.${ImageTagTable.TAG_ID} = t.${TagTable.TAG_ID}
GROUP BY i.${ImageTable.ID}
    """,
    )
    suspend fun getRelations(): List<DBImageTagRelationForBackup>

    @Query(
        "DELETE FROM ${ImageTagTable.TABLE} WHERE ${ImageTagTable.IMAGE_ID} IN (:imageIds)",
    )
    fun deleteByImages(vararg imageIds: Long)
}
