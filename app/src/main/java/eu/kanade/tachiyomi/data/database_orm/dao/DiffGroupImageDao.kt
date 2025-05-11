package eu.kanade.tachiyomi.data.database_orm.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import eu.kanade.tachiyomi.data.database.tables.DiffGroupImageTable
import eu.kanade.tachiyomi.data.database.tables.DiffGroupTable
import eu.kanade.tachiyomi.data.database_orm.models.DBDiffGroup
import eu.kanade.tachiyomi.data.database_orm.models.DBDiffGroupImage
import kotlinx.coroutines.flow.Flow

@Dao
interface DiffGroupImageDao : BaseDao<DBDiffGroup> {

    @Query("SELECT * FROM ${DiffGroupImageTable.TABLE} WHERE ${DiffGroupImageTable.GROUP_ID} = :groupId ORDER BY ${DiffGroupImageTable.SORT_ORDER}")
    suspend fun getByGroupId(groupId: String): List<DBDiffGroupImage>

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
}
