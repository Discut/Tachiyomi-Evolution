package eu.kanade.tachiyomi.data.gallery

import eu.kanade.tachiyomi.data.database_orm.GalleryDatabase
import eu.kanade.tachiyomi.data.database_orm.dao.TagDao
import eu.kanade.tachiyomi.data.database_orm.models.DBImageAndTag
import eu.kanade.tachiyomi.data.database_orm.models.DBImageTagRelationForBackup
import eu.kanade.tachiyomi.data.database_orm.models.DBTag
import eu.kanade.tachiyomi.data.database_orm.models.DBTagType
import eu.kanade.tachiyomi.source.gallery.local.LocalGallerySource
import eu.kanade.tachiyomi.util.generateTimestampBasedID
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class BackupManagerImpl(
    private val room: GalleryDatabase = Injekt.get(),
    private val json: Json = Injekt.get(),
) : IBackupManager {
    override fun backupToDevice(path: String): Flow<BackupResult> =
        flow {
            val relations: List<DBImageTagRelationForBackup>
            try {
                relations = withIOContext {
                    room.getImageAndTagDao().getRelations()
                }
                val encodeToString = json.encodeToString(relations)
                // 在文件中加入时间
                val fileName =
                    SimpleDateFormat("yyyy-MM-dd_HH_mm_ss", Locale.getDefault()).format(Date())
                        .let {
                            "backup-$it.galleryBackup"
                        }
                val file = File(path, fileName)
                if (file.exists()) {
                    file.delete()
                }
                withContext(Dispatchers.IO) {
                    file.createNewFile()
                }
                file.writeText(encodeToString)
            } catch (e: Exception) {
                emit(BackupResult.Error(e.message ?: "Unknown error"))
                Timber.e(e)
                return@flow
            }
            emit(BackupResult.Success(relations.size.toString()))
        }

    override fun restoreFromDevice(path: String): Flow<BackupResult> = flow {
        if (!path.endsWith(".galleryBackup")) {
            emit(BackupResult.Error("Invalid backup file"))
            return@flow
        }
        // 合并进度更新逻辑
        suspend fun emitProgress(index: Int, total: Int) {
            if (index % 100 == 0 || index == total) {
                emit(BackupResult.Progress(index, total))
            }
        }

        try {
            val file = File(path)
            val relations =
                json.decodeFromString<List<DBImageTagRelationForBackup>>(file.readText())
            // 开始准备恢复
            emit(BackupResult.Progress(0, relations.size))

            val imageDao = room.getImageDao()
            val tagDao = room.getTagDao()
            val imageAndTagDao = room.getImageAndTagDao()

            val allTagValues = relations.mapNotNull { it.tagValue }.distinct()
            val tagsMap = updateTags(tagDao, allTagValues).associate {
                it.tagValue to it.tagId
            }

            relations.forEachIndexed { index, relation ->
                val tagValue = relation.tagValue
                val tagId = tagsMap[tagValue]
                val filePath = relation.filePath
                tagId?.apply {
                    val idsByPath = imageDao.getIdsByPath(filePath)
                    if (idsByPath.isNotEmpty()) {
                        imageDao.getById(idsByPath.first())?.apply {
                            imageAndTagDao.insertOrUpdate(
                                DBImageAndTag(
                                    imageId = this.id,
                                    tagId = tagId,
                                ),
                            )
                        }
                    }
                }
                emitProgress(index + 1, relations.size)
            }
            emit(BackupResult.Success("Restored ${relations.size} relations"))
        } catch (e: Exception) {
            emit(BackupResult.Error(e.message ?: "Unknown error"))
            Timber.e(e)
        }
    }.buffer(Channel.UNLIMITED).catch { e ->
        emit(BackupResult.Error(e.message ?: "Unknown error"))
    }

    private fun updateTags(dao: TagDao, tags: List<String>): List<DBTag> {
        val result = mutableListOf<DBTag>()
        room.runInTransaction {
            tags.distinct()
                .filter { it.isNotBlank() }
                .forEach {
                    val id: Long
                    val ids = dao.getIdsByName(it)
                    if (ids.isEmpty()) {
                        val newTagId = generateTimestampBasedID()
                        dao.insertOrUpdateWithDeferred(
                            DBTag(
                                tagValue = it,
                                typeId = DBTagType.PresetType.OTHER.id.toLong(),
                                tagId = newTagId,
                                source = LocalGallerySource.ID,
                            ),
                        )
                        id = newTagId
                    } else {
                        id = ids.first()
                    }
                    dao.getTagByIdWithDeferred(id)?.apply {
                        result.add(this)
                    }
                }
        }
        return result
    }
}
