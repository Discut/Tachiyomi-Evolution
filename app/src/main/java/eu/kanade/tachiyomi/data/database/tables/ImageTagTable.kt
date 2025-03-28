package eu.kanade.tachiyomi.data.database.tables

/*
-- 图片-标签关联表（多对多关系）
CREATE TABLE IF NOT EXISTS image_tags
(
    image_id INTEGER NOT NULL REFERENCES images (id) ON DELETE CASCADE,
    tag_id   INTEGER NOT NULL REFERENCES tags (tag_id) ON DELETE CASCADE,
    PRIMARY KEY (image_id, tag_id)
);
 */
object ImageTagTable {

    const val TABLE = "image_tags"

    const val IMAGE_ID = "image_id"

    const val TAG_ID = "tag_id"

    val createTableQuery: String
        get() =
            """CREATE TABLE IF NOT EXISTS $TABLE(
            $IMAGE_ID INTEGER NOT NULL REFERENCES images (id) ON DELETE CASCADE,
            $TAG_ID INTEGER NOT NULL REFERENCES tags (tag_id) ON DELETE CASCADE,
            PRIMARY KEY ($IMAGE_ID, $TAG_ID)
            )"""
}
