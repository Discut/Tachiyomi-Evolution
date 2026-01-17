package eu.kanade.tachiyomi.data.database.tables

/**
 * 动画序列表定义
 *
 * 存储关键帧序列数据，用于图片的幻灯片动画效果
 */
object AnimationSequenceTable {

    const val TABLE = "animation_sequences"

    const val ID = "sequence_id"

    const val IMAGE_ID = "image_id"

    const val NAME = "name"

    const val DURATION_MS = "duration_ms"

    const val DATA = "data"

    const val VERSION = "version"

    const val EXPORTED = "exported"

    const val CREATED_AT = "created_at"

    const val UPDATED_AT = "updated_at"

    /**
     * 创建表的 SQL 语句
     *
     * 注意：外键和索引由 Room 的 @Entity 注解自动处理，这里不需要手动定义
     */
    val createTableQuery: String
        get() =
            """CREATE TABLE IF NOT EXISTS $TABLE(
            $ID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            $IMAGE_ID INTEGER NOT NULL,
            $NAME TEXT NOT NULL CHECK (length($NAME) > 0),
            $DURATION_MS INTEGER NOT NULL CHECK ($DURATION_MS > 0),
            $DATA TEXT NOT NULL CHECK (length($DATA) > 0),
            $VERSION INTEGER NOT NULL DEFAULT 1,
            $EXPORTED INTEGER NOT NULL DEFAULT 0,
            $CREATED_AT INTEGER NOT NULL DEFAULT (STRFTIME('%s', 'NOW') * 1000),
            $UPDATED_AT INTEGER NOT NULL DEFAULT (STRFTIME('%s', 'NOW') * 1000)
            )"""

    /**
     * 创建触发器的 SQL 语句
     *
     * 索引由 Room 自动处理，只需要创建更新时间戳的触发器
     */
    val createTriggerStatements: List<String>
        get() = listOf(
            // 更新时间戳触发器
            "CREATE TRIGGER IF NOT EXISTS update_animation_sequence_timestamp AFTER UPDATE ON $TABLE FOR EACH ROW BEGIN UPDATE $TABLE SET $UPDATED_AT = (STRFTIME('%s', 'NOW') * 1000) WHERE $ID = OLD.$ID; END",
        )
}
