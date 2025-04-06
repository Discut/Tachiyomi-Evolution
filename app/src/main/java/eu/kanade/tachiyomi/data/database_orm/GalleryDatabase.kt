package eu.kanade.tachiyomi.data.database_orm

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import eu.kanade.tachiyomi.data.database_orm.dao.ImageAndTagDao
import eu.kanade.tachiyomi.data.database_orm.dao.ImageDao
import eu.kanade.tachiyomi.data.database_orm.dao.TagDao
import eu.kanade.tachiyomi.data.database_orm.dao.TagTypeDao
import eu.kanade.tachiyomi.data.database_orm.models.DBDiffGroup
import eu.kanade.tachiyomi.data.database_orm.models.DBImage
import eu.kanade.tachiyomi.data.database_orm.models.DBImageAndTag
import eu.kanade.tachiyomi.data.database_orm.models.DBTag
import eu.kanade.tachiyomi.data.database_orm.models.DBTagType

@Database(
    entities = [
        DBDiffGroup::class,
        DBImage::class,
        DBImageAndTag::class,
        DBTag::class,
        DBTagType::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun getImageDao(): ImageDao

    abstract fun getTagDao(): TagDao

    abstract fun getTagTypeDao(): TagTypeDao

    abstract fun getImageAndTagDao(): ImageAndTagDao

    companion object {
        // 数据库初始化配置
        private val initCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 插入预置数据（使用事务保证原子性）
                db.beginTransaction()
                try {
                    db.execSQL(
                        """
                INSERT OR IGNORE INTO tag_types (type_id, type_name)
                VALUES 
                    (1, 'author'),
                    (2, 'source'),
                    (3, 'other')
                        """.trimIndent(),
                    )
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }

        fun getDatabase(context: Context): GalleryDatabase {
            return Room
                .databaseBuilder(context, GalleryDatabase::class.java, "gallery.db")
                .addCallback(initCallback)
                .build()
        }
    }
}
