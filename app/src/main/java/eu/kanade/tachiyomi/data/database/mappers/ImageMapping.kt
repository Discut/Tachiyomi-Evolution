package eu.kanade.tachiyomi.data.database.mappers

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.operations.put.DefaultPutResolver
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.InsertQuery
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.models.DBImage
import eu.kanade.tachiyomi.data.database.tables.ImageTable

class ImageMapping : SQLiteTypeMapping<DBImage>(
    ImagePutResolver(),
    ImageGetResolver(),
    ImageDeleteResolver(),
)

class ImagePutResolver : DefaultPutResolver<DBImage>() {
    override fun mapToInsertQuery(`object`: DBImage): InsertQuery {
        return InsertQuery.builder()
            .table(ImageTable.TABLE)
            .build()
    }

    override fun mapToUpdateQuery(`object`: DBImage): UpdateQuery {
        return UpdateQuery.builder()
            .table(ImageTable.TABLE)
            .where("${ImageTable.ID} = ?")
            .whereArgs(`object`.id)
            .build()
    }

    /*
-- 图片元数据表（核心实体）
CREATE TABLE IF NOT EXISTS images
(
    id              INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
    file_path       TEXT     NOT NULL UNIQUE CHECK (length(file_path) > 0), -- 文件绝对路径
    collection_path TEXT     NOT NULL CHECK (collection_path LIKE '/%'),    -- 合集路径（Linux风格）
    filesize        INTEGER  NOT NULL CHECK (filesize > 0),                 -- 文件大小（字节）
    checksum        TEXT CHECK (length(checksum) = 64),                     -- SHA256校验（可选）
    exif_json       TEXT,                                                   -- EXIF元数据（JSON格式）
    created_at      DATETIME NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')),
    modified_at     DATETIME NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))
);
     */
    override fun mapToContentValues(`object`: DBImage): ContentValues {
        return ContentValues(10).apply {
            put(ImageTable.ID, `object`.id)
            put(ImageTable.FILE_PATH, `object`.filePath)
            put(ImageTable.COLLECTION_PATH, `object`.collectionPath)
            put(ImageTable.FILE_SIZE, `object`.fileSize)
            put(ImageTable.CHECKSUM, `object`.checksum)
            put(ImageTable.EXIF_JSON, `object`.exifJson)
            put(ImageTable.CREATED_AT, `object`.createdAt)
            put(ImageTable.MODIFIED_AT, `object`.modifiedAt)
            put(ImageTable.WIDTH, `object`.width)
            put(ImageTable.HEIGHT, `object`.height)
        }
    }
}

class ImageGetResolver : DefaultGetResolver<DBImage>() {
    @SuppressLint("Range")
    override fun mapFromCursor(cursor: Cursor): DBImage {
        return DBImage().apply {
            id = cursor.getLong(cursor.getColumnIndex(ImageTable.ID))
            filePath = cursor.getString(cursor.getColumnIndex(ImageTable.FILE_PATH))
            collectionPath = cursor.getString(cursor.getColumnIndex(ImageTable.COLLECTION_PATH))
            fileSize = cursor.getLong(cursor.getColumnIndex(ImageTable.FILE_SIZE))
            checksum = cursor.getString(cursor.getColumnIndex(ImageTable.CHECKSUM))
            exifJson = cursor.getString(cursor.getColumnIndex(ImageTable.EXIF_JSON))
            createdAt = cursor.getString(cursor.getColumnIndex(ImageTable.CREATED_AT))
            modifiedAt = cursor.getString(cursor.getColumnIndex(ImageTable.MODIFIED_AT))
            width = cursor.getInt(cursor.getColumnIndex(ImageTable.WIDTH))
            height = cursor.getInt(cursor.getColumnIndex(ImageTable.HEIGHT))
        }
    }
}

class ImageDeleteResolver : DefaultDeleteResolver<DBImage>() {
    override fun mapToDeleteQuery(`object`: DBImage): DeleteQuery {
        return DeleteQuery.builder()
            .table(ImageTable.TABLE)
            .where("${ImageTable.ID} = ?")
            .whereArgs(`object`.id)
            .build()
    }
}
