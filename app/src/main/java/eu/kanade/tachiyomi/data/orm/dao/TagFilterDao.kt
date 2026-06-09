package eu.kanade.tachiyomi.data.orm.dao

import androidx.room.Dao
import androidx.room.Query
import eu.kanade.tachiyomi.data.database.tables.AITagFilterTable
import eu.kanade.tachiyomi.data.orm.models.DBTagFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface TagFilterDao : BaseDao<DBTagFilter> {

    @Query("SELECT * FROM ${AITagFilterTable.TABLE}")
    suspend fun getAll(): List<DBTagFilter>

    @Query("SELECT * FROM ${AITagFilterTable.TABLE}")
    fun getAllAsFlow(): Flow<List<DBTagFilter>>

    @Query("DELETE FROM ${AITagFilterTable.TABLE} WHERE ${AITagFilterTable.TAG_NAME} = :name")
    suspend fun deleteByName(name: String): Int

    @Query("SELECT * FROM ${AITagFilterTable.TABLE} WHERE ${AITagFilterTable.TAG_NAME} LIKE '%' || :query || '%'")
    suspend fun searchByName(query: String): List<DBTagFilter>
}
