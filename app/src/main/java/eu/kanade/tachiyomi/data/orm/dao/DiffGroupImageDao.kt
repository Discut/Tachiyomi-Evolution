package eu.kanade.tachiyomi.data.orm.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import eu.kanade.tachiyomi.data.database.tables.DiffGroupImageTable
import eu.kanade.tachiyomi.data.database.tables.DiffGroupTable
import eu.kanade.tachiyomi.data.database.tables.ImageTable
import eu.kanade.tachiyomi.data.orm.models.DBDiffGroup
import eu.kanade.tachiyomi.data.orm.models.DBDiffGroupImage
import eu.kanade.tachiyomi.data.orm.models.DBImage
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface DiffGroupImageDao : BaseDao<DBDiffGroup> {

    @Query("SELECT * FROM ${DiffGroupImageTable.TABLE} WHERE ${DiffGroupImageTable.GROUP_ID} = :groupId ORDER BY ${DiffGroupImageTable.SORT_ORDER}")
    suspend fun getByGroupId(groupId: String): List<DBDiffGroupImage>

    @Query(
        """
            SELECT * FROM ${ImageTable.TABLE} WHERE ${ImageTable.ID} IN (
                SELECT ${DiffGroupImageTable.IMAGE_ID} FROM ${DiffGroupImageTable.TABLE}
                WHERE ${DiffGroupImageTable.GROUP_ID} = :groupId
            )
        """,
    )
    suspend fun getImagesByGroupId(groupId: String): List<DBImage>

    @Query("SELECT * FROM ${DiffGroupImageTable.TABLE} WHERE ${DiffGroupImageTable.IMAGE_ID} = :imageId ORDER BY ${DiffGroupImageTable.SORT_ORDER}")
    suspend fun getByImageId(imageId: Long): List<DBDiffGroupImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllImages(vararg entities: DBDiffGroupImage)

    @Delete
    suspend fun deleteImage(entity: DBDiffGroupImage): Int

    @Query("SELECT * FROM ${DiffGroupImageTable.TABLE}")
    fun getAllDiffImagesAsFlow(): Flow<List<DBDiffGroupImage>>

    @Query("SELECT * FROM ${DiffGroupTable.TABLE} WHERE ${DiffGroupTable.GROUP_ID} = :groupId")
    suspend fun getGroupById(groupId: String): DBDiffGroup?

    @Query("UPDATE ${DiffGroupImageTable.TABLE} SET ${DiffGroupImageTable.SORT_ORDER} = :sortOrder WHERE ${DiffGroupImageTable.IMAGE_ID} = :imageId AND ${DiffGroupImageTable.GROUP_ID} = :groupId")
    suspend fun updateDiffGroupImageOrder(
        groupId: String,
        imageId: Long,
        sortOrder: BigDecimal,
    ): Int
}
