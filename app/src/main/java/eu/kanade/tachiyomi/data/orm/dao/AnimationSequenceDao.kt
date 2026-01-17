package eu.kanade.tachiyomi.data.orm.dao

import androidx.room.Dao
import androidx.room.Query
import eu.kanade.tachiyomi.data.database.tables.AnimationSequenceTable
import eu.kanade.tachiyomi.data.orm.models.AnimationSequence
import kotlinx.coroutines.flow.Flow

/**
 * 动画序列数据访问对象
 */
@Dao
interface AnimationSequenceDao : BaseDao<AnimationSequence> {

    /**
     * 获取所有动画序列（Flow）
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} ORDER BY ${AnimationSequenceTable.CREATED_AT} DESC",
    )
    fun getAllAsFlow(): Flow<List<AnimationSequence>>

    /**
     * 获取所有动画序列
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} ORDER BY ${AnimationSequenceTable.CREATED_AT} DESC",
    )
    fun getAll(): List<AnimationSequence>

    /**
     * 根据 ID 获取动画序列（Flow）
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.ID} = :id",
    )
    fun getByIdAsFlow(id: Long): Flow<AnimationSequence?>

    /**
     * 根据 ID 获取动画序列
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.ID} = :id",
    )
    fun getById(id: Long): AnimationSequence?

    /**
     * 根据图片 ID 获取所有关联的动画序列（Flow）
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.IMAGE_ID} = :imageId ORDER BY ${AnimationSequenceTable.CREATED_AT} DESC",
    )
    fun getByImageIdAsFlow(imageId: Long): Flow<List<AnimationSequence>>

    /**
     * 根据图片 ID 获取所有关联的动画序列
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.IMAGE_ID} = :imageId ORDER BY ${AnimationSequenceTable.CREATED_AT} DESC",
    )
    fun getByImageId(imageId: Long): List<AnimationSequence>

    /**
     * 根据图片 ID 获取第一个动画序列
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.IMAGE_ID} = :imageId ORDER BY ${AnimationSequenceTable.CREATED_AT} DESC LIMIT 1",
    )
    fun getFirstByImageId(imageId: Long): AnimationSequence?

    /**
     * 根据图片 ID 获取第一个动画序列（Flow）
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.IMAGE_ID} = :imageId ORDER BY ${AnimationSequenceTable.CREATED_AT} DESC LIMIT 1",
    )
    fun getFirstByImageIdAsFlow(imageId: Long): Flow<AnimationSequence?>

    /**
     * 更新导出状态
     */
    @Query(
        "UPDATE ${AnimationSequenceTable.TABLE} SET ${AnimationSequenceTable.EXPORTED} = :exported, ${AnimationSequenceTable.UPDATED_AT} = :updatedAt WHERE ${AnimationSequenceTable.ID} = :id",
    )
    suspend fun updateExportedStatus(id: Long, exported: Boolean, updatedAt: Long = System.currentTimeMillis()): Int

    /**
     * 删除指定图片的所有动画序列
     */
    @Query(
        "DELETE FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.IMAGE_ID} = :imageId",
    )
    suspend fun deleteByImageId(imageId: Long): Int

    /**
     * 统计指定图片的动画序列数量
     */
    @Query(
        "SELECT COUNT(*) FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.IMAGE_ID} = :imageId",
    )
    fun countByImageId(imageId: Long): Int

    /**
     * 统计所有动画序列数量
     */
    @Query(
        "SELECT COUNT(*) FROM ${AnimationSequenceTable.TABLE}",
    )
    fun count(): Long

    /**
     * 获取所有未导出的动画序列
     */
    @Query(
        "SELECT * FROM ${AnimationSequenceTable.TABLE} WHERE ${AnimationSequenceTable.EXPORTED} = 0 ORDER BY ${AnimationSequenceTable.CREATED_AT} DESC",
    )
    fun getUnexported(): List<AnimationSequence>
}
