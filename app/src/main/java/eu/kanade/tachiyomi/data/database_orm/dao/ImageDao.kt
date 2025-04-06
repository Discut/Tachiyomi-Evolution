package eu.kanade.tachiyomi.data.database_orm.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import eu.kanade.tachiyomi.data.database.tables.ImageTable
import eu.kanade.tachiyomi.data.database.tables.ImageTagTable
import eu.kanade.tachiyomi.data.database_orm.models.DBImage
import eu.kanade.tachiyomi.data.database_orm.models.ImageWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao : BaseDao<DBImage> {

    @Query(
        "SELECT * FROM ${ImageTable.TABLE}",
    )
    fun getAllAsFlow(): Flow<List<DBImage>>

    @Query(
        "SELECT * FROM ${ImageTable.TABLE}",
    )
    fun getAll(): List<DBImage>

    @Query(
        "SELECT * FROM ${ImageTable.TABLE} WHERE ${ImageTable.ID} = :id",
    )
    fun getByIdAsFlow(id: Long): Flow<DBImage?>

    @Query(
        "SELECT * FROM ${ImageTable.TABLE} WHERE ${ImageTable.ID} = :id",
    )
    fun getById(id: Long): DBImage?

    @Transaction
    @Query(
        "SELECT * FROM ${ImageTable.TABLE} WHERE ${ImageTable.ID} = :id",
    )
    suspend fun getImageWithTags(id: Long): ImageWithTags?

    @Transaction
    @Query(
        "SELECT * FROM ${ImageTable.TABLE} WHERE ${ImageTable.ID} = :id",
    )
    fun getImageWithTagsAsFlow(id: Long): Flow<ImageWithTags?>

    @Query(
        "DELETE FROM ${ImageTagTable.TABLE} WHERE ${ImageTagTable.IMAGE_ID} = :imageId AND ${ImageTagTable.TAG_ID} = :tagId",
    )
    suspend fun deleteImageAndTags(imageId: Long, tagId: Long)
}
