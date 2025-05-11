package eu.kanade.tachiyomi.data.database_orm.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import eu.kanade.tachiyomi.data.database.tables.TagTable
import eu.kanade.tachiyomi.data.database_orm.models.DBTag
import eu.kanade.tachiyomi.data.database_orm.models.TagWithImages
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao : BaseDao<DBTag> {

    @Query(
        "SELECT * FROM ${TagTable.TABLE}",
    )
    fun getAllAsFlow(): Flow<List<DBTag>>

    @Transaction
    @Query(
        "SELECT * FROM ${TagTable.TABLE} WHERE ${TagTable.TAG_ID} = :tagId",
    )
    suspend fun getTagWithImages(tagId: Long): TagWithImages?

    @Transaction
    @Query(
        "SELECT * FROM ${TagTable.TABLE} ",
    )
    fun getTagsWithImagesAsFlow(): Flow<List<TagWithImages>>

    @Transaction
    @Query(
        "SELECT * FROM ${TagTable.TABLE} WHERE ${TagTable.TAG_ID} = :tagId",
    )
    fun getTagWithImagesAsFlow(tagId: Long): Flow<TagWithImages?>

    @Query(
        "SELECT * FROM ${TagTable.TABLE} WHERE ${TagTable.TAG_ID} = :tagId",
    )
    suspend fun getTagById(tagId: Long): DBTag?

    @Query(
        "SELECT * FROM ${TagTable.TABLE} WHERE ${TagTable.TAG_ID} = :tagId",
    )
    fun getTagByIdWithDeferred(tagId: Long): DBTag?

    @Query(
        "SELECT ${TagTable.TAG_ID} FROM ${TagTable.TABLE} WHERE ${TagTable.TAG_VALUE} = :name",
    )
    fun getIdsByName(name: String): List<Long>
}
