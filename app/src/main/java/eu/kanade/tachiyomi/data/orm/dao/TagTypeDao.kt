package eu.kanade.tachiyomi.data.orm.dao

import androidx.room.Dao
import androidx.room.Query
import eu.kanade.tachiyomi.data.database.tables.TagTypeTable
import eu.kanade.tachiyomi.data.orm.models.DBTagType

@Dao
interface TagTypeDao : BaseDao<DBTagType> {

    @Query(
        "SELECT * FROM ${TagTypeTable.TABLE} WHERE ${TagTypeTable.TYPE_ID} = :id",
    )
    suspend fun getById(id: Long): DBTagType?

    @Query(
        "SELECT * FROM ${TagTypeTable.TABLE}",
    )
    suspend fun getAll(): List<DBTagType>
}
