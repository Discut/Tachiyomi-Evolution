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
import eu.kanade.tachiyomi.data.database.models.DBTagType
import eu.kanade.tachiyomi.data.database.tables.TagTypeTable

class TagTypeMapping : SQLiteTypeMapping<DBTagType>(
    TagTypePutResolver(),
    TagTypeGetResolver(),
    TagTypeDeleteResolver(),
)

class TagTypePutResolver : DefaultPutResolver<DBTagType>() {
    override fun mapToInsertQuery(`object`: DBTagType): InsertQuery {
        return InsertQuery.builder()
            .table(TagTypeTable.TABLE)
            .build()
    }

    override fun mapToUpdateQuery(`object`: DBTagType): UpdateQuery {
        return UpdateQuery.builder()
            .table(TagTypeTable.TABLE)
            .where("${TagTypeTable.TYPE_ID} = ?")
            .whereArgs(`object`.typeId)
            .build()
    }

    override fun mapToContentValues(`object`: DBTagType): ContentValues {
        return ContentValues(2).apply {
            put(TagTypeTable.TYPE_NAME, `object`.typeName)
            put(TagTypeTable.TYPE_ID, `object`.typeId)
        }
    }
}

class TagTypeGetResolver : DefaultGetResolver<DBTagType>() {
    @SuppressLint("Range")
    override fun mapFromCursor(cursor: Cursor): DBTagType {
        return DBTagType().apply {
            typeId = cursor.getLong(cursor.getColumnIndex(TagTypeTable.TYPE_ID))
            typeName = cursor.getString(cursor.getColumnIndex(TagTypeTable.TYPE_NAME))
        }
    }
}

class TagTypeDeleteResolver : DefaultDeleteResolver<DBTagType>() {
    override fun mapToDeleteQuery(`object`: DBTagType): DeleteQuery {
        return DeleteQuery.builder()
            .table(TagTypeTable.TABLE)
            .where("${TagTypeTable.TYPE_ID} = ?")
            .whereArgs(`object`.typeId)
            .build()
    }
}
