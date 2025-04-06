package eu.kanade.tachiyomi.data.database_orm.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import eu.kanade.tachiyomi.data.database.tables.ImageTagTable
import eu.kanade.tachiyomi.data.database_orm.models.DBImageAndTag

@Dao
interface ImageAndTagDao : BaseDao<DBImageAndTag> {

    @Query(
        "SELECT * FROM ${ImageTagTable.TABLE}",
    )
    fun getAll(): List<DBImageAndTag>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: DBImageAndTag)
}
