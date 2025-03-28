package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.DBTag
import eu.kanade.tachiyomi.data.database.tables.TagTable

interface TagQueries : DbProvider {
    fun getTags() = db.get()
        .listOfObjects(DBTag::class.java)
        .withQuery(
            Query.builder()
                .table(TagTable.TABLE)
                .orderBy(TagTable.TAG_VALUE)
                .build(),
        )
        .prepare()
}
