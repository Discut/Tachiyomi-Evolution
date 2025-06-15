package eu.kanade.tachiyomi.data.orm.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import eu.kanade.tachiyomi.data.database.tables.DiffGroupImageTable.GROUP_ID
import eu.kanade.tachiyomi.data.database.tables.DiffGroupImageTable.IMAGE_ID
import eu.kanade.tachiyomi.data.database.tables.DiffGroupImageTable.SORT_ORDER
import eu.kanade.tachiyomi.data.database.tables.DiffGroupImageTable.TABLE
import eu.kanade.tachiyomi.data.database.tables.ImageTable

val migrationObjets = listOf(
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE ${ImageTable.TABLE} ADD COLUMN ${ImageTable.IS_HIDE} INTEGER NOT NULL DEFAULT 0")
        }
    },

    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE(
            $IMAGE_ID INTEGER NOT NULL REFERENCES images (image_id) ON DELETE CASCADE,
            $GROUP_ID TEXT NOT NULL REFERENCES diff_groups (group_id) ON DELETE CASCADE,
            $SORT_ORDER TEXT NOT NULL DEFAULT '1000',
            PRIMARY KEY ($GROUP_ID, $IMAGE_ID));
                """.trimIndent(),
            )
            // -- 为反向查询添加索引
            db.execSQL(
                """
                CREATE INDEX idx_diff_group_image_id ON diff_group_images ($IMAGE_ID);
                """.trimIndent(),
            )
            // -- 为排序查询添加复合索引
            db.execSQL(
                """
                CREATE INDEX idx_diff_group_sort ON diff_group_images ($GROUP_ID, $SORT_ORDER);
                """.trimIndent(),
            )
            // -- 索引优化（根据查询模式设计）
            db.execSQL(
                """
                CREATE INDEX idx_diff_group_order ON diff_group_images ($SORT_ORDER);
                """.trimIndent(),
            )
        }
    },
)
