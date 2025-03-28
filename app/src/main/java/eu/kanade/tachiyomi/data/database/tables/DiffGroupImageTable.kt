package eu.kanade.tachiyomi.data.database.tables

/*
-- 差分组-图片关联表（带排序功能）
CREATE TABLE IF NOT EXISTS diff_group_images
(
    group_id   TEXT    NOT NULL REFERENCES diff_groups (group_id) ON DELETE CASCADE,
    image_id   INTEGER NOT NULL REFERENCES images (id) ON DELETE CASCADE,
    sort_order NUMERIC(18,6) NOT NULL CHECK (sort_order >= 0),
    PRIMARY KEY (group_id, image_id)
);
 */
object DiffGroupImageTable {

    const val TABLE = "diff_group_images"

    const val GROUP_ID = "group_id"

    const val IMAGE_ID = "image_id"

    const val SORT_ORDER = "sort_order"

    val createTableQuery: String
        get() =
            """CREATE TABLE IF NOT EXISTS $TABLE(
            $GROUP_ID TEXT NOT NULL REFERENCES diff_groups (group_id) ON DELETE CASCADE,
            $IMAGE_ID INTEGER NOT NULL REFERENCES images (id) ON DELETE CASCADE,
            $SORT_ORDER NUMERIC(18,6) NOT NULL CHECK ($SORT_ORDER >= 0),
            PRIMARY KEY ($GROUP_ID, $IMAGE_ID)
            )"""

    val createIndexSql: String
        get() =
            """
                -- 索引优化（根据查询模式设计）
                CREATE INDEX IF NOT EXISTS idx_diff_group_order ON diff_group_images ($SORT_ORDER);
                -- 为排序查询添加复合索引
                CREATE INDEX idx_diff_group_sort ON diff_group_images ($GROUP_ID, $SORT_ORDER);
                -- 为反向查询添加索引
                CREATE INDEX idx_diff_group_image_id ON diff_group_images ($IMAGE_ID);
                
                -- 防止排序值溢出
                CREATE TRIGGER prevent_sort_overflow
                    BEFORE UPDATE OF $SORT_ORDER ON $TABLE
                    WHEN NEW.$SORT_ORDER > 1e18 OR NEW.$SORT_ORDER < 0
                BEGIN
                    SELECT RAISE(ABORT, 'Invalid sort_order value');
                END;
            """
}
