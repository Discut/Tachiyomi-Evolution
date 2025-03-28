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
import eu.kanade.tachiyomi.data.database.models.DBTag
import eu.kanade.tachiyomi.data.database.tables.TagTable

class TagMapping : SQLiteTypeMapping<DBTag>(
    TagPutResolver(),
    TagGetResolver(),
    TagDeleteResolver(),
)

class TagPutResolver : DefaultPutResolver<DBTag>() {
    override fun mapToInsertQuery(`object`: DBTag): InsertQuery {
        return InsertQuery.builder()
            .table(TagTable.TABLE)
            .build()
    }

    override fun mapToUpdateQuery(`object`: DBTag): UpdateQuery {
        return UpdateQuery.builder()
            .table(TagTable.TABLE)
            .where("${TagTable.TAG_ID} = ?")
            .whereArgs(`object`.tagId)
            .build()
    }

    override fun mapToContentValues(`object`: DBTag): ContentValues {
        return ContentValues(3).apply {
            put(TagTable.TAG_ID, `object`.tagId)
            put(TagTable.TYPE_ID, `object`.typeId)
            put(TagTable.TAG_VALUE, `object`.tagValue)
        }
    }
}

class TagGetResolver : DefaultGetResolver<DBTag>() {
    @SuppressLint("Range")
    override fun mapFromCursor(cursor: Cursor): DBTag {
        return DBTag().apply {
            tagId = cursor.getLong(cursor.getColumnIndex(TagTable.TAG_ID))
            typeId = cursor.getLong(cursor.getColumnIndex(TagTable.TYPE_ID))
            tagValue = cursor.getString(cursor.getColumnIndex(TagTable.TAG_VALUE))
        }
    }
}

class TagDeleteResolver : DefaultDeleteResolver<DBTag>() {
    override fun mapToDeleteQuery(`object`: DBTag): DeleteQuery {
        return DeleteQuery.builder()
            .table(TagTable.TABLE)
            .where("${TagTable.TAG_ID} = ?")
            .whereArgs(`object`.tagId)
            .build()
    }
}
