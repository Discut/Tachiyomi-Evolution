package eu.kanade.tachiyomi.data.orm.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface BaseDao<T> {
    @Delete
    suspend fun delete(entity: T): Int

    @Update
    suspend fun update(entity: T): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWithDeferred(entity: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg entities: T)

    // 插入时自动替换已存在项
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(vararg entities: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateWithDeferred(entity: T)
}
