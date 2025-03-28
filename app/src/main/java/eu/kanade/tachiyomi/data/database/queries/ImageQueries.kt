package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.DBImage
import eu.kanade.tachiyomi.data.database.tables.ImageTable

interface ImageQueries : DbProvider {
    fun getAllImages() = db.get()
        .listOfObjects(DBImage::class.java)
        .withQuery(
            Query.builder()
                .table(ImageTable.TABLE)
                .orderBy(ImageTable.CREATED_AT)
                .build(),
        )
        .prepare()

    fun insertImage(image: DBImage) = db.put()
        .`object`(image)
        .prepare()
    fun insertImages(images: List<DBImage>) = db.put()
        .objects(images)
        .prepare()
}
