package eu.kanade.tachiyomi.data.orm.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import eu.kanade.tachiyomi.data.database.tables.AnimationSequenceTable
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

    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 创建动画序列表
            db.execSQL(AnimationSequenceTable.createTableQuery)

            // 创建触发器（索引由 Room 自动创建）
            AnimationSequenceTable.createTriggerStatements.forEach { sql ->
                db.execSQL(sql)
            }
        }
    },

    // 版本 4 -> 5: AnimationSequence 主键添加 autoGenerate
    // 表结构不变，只是 Room 现在会在插入时自动生成 ID
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 不需要任何 SQL 操作，表结构不变
        }
    },

    // 版本 5 -> 6: 新增 'auto' 标签类型 (AI 打标)
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO tag_types (type_id, type_name) VALUES (4, 'auto')",
            )
        }
    },

    // 版本 6 -> 7: 新增 AI 标签过滤表
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_tag_filters (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    tag_name TEXT NOT NULL UNIQUE,
                    created_at INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
        }
    },
)
