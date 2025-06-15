package eu.kanade.tachiyomi.ui.gallery.diffgroup

import androidx.room.withTransaction
import eu.kanade.tachiyomi.data.orm.GalleryDatabase
import eu.kanade.tachiyomi.data.orm.models.DBDiffGroupImage
import eu.kanade.tachiyomi.model.toImageBO
import eu.kanade.tachiyomi.ui.base.viewmodel.BaseViewModel
import eu.kanade.tachiyomi.ui.gallery.diffgroup.domain.DiffGroupEffect
import eu.kanade.tachiyomi.ui.gallery.diffgroup.domain.DiffGroupEvent
import eu.kanade.tachiyomi.ui.gallery.diffgroup.domain.DiffGroupState
import eu.kanade.tachiyomi.ui.gallery.diffgroup.domain.DiffImageVo
import eu.kanade.tachiyomi.util.system.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DiffGroupViewModel(
    private val room: GalleryDatabase = Injekt.get(),
) : BaseViewModel<DiffGroupState, DiffGroupEvent, DiffGroupEffect>() {

    private suspend fun init(diffGroupId: String): DiffGroupState {
        val diffGroupDao = room.getDiffGroupDao()
        val diffGroup = diffGroupDao.getGroupById(diffGroupId)
            ?: return DiffGroupState.Error("DiffGroup not found")

        val diffImages = diffGroupDao.getByGroupId(diffGroupId)
        val dbImages = diffGroupDao.getImagesByGroupId(diffGroupId)
        return DiffGroupState.Content(
            diffGroup = diffGroup,
            images = diffImages.mapNotNull { diffImage ->
                dbImages.find { it.id == diffImage.imageId }?.let {
                    DiffImageVo(
                        diffGroupId = diffGroupId,
                        image = it.toImageBO(order = diffImage.sortOrder),
                    )
                }
            },
        )
    }

    override fun initialState(): DiffGroupState = DiffGroupState.Loading

    override suspend fun handleEvent(
        event: DiffGroupEvent,
        state: DiffGroupState,
    ): DiffGroupState = when (event) {
        is DiffGroupEvent.Init -> {
            init(event.diffGroupId)
        }

        is DiffGroupEvent.ChangeOrder -> {
            withIOContext {
                val diffGroupDao = room.getDiffGroupDao()
                val groupImages =
                    event.changed.filter { ch -> event.original.find { it.image == ch.image && it.order != ch.order } != null }
                        .map {
                            DBDiffGroupImage(
                                imageId = it.image.id,
                                groupId = it.diffGroupId,
                                sortOrder = it.order,
                            )
                        }
                room.withTransaction {
                    groupImages.forEach {
                        diffGroupDao.updateDiffGroupImageOrder(it.groupId, it.imageId, it.sortOrder)
                    }
                }
                init(diffGroupId = event.changed.first().diffGroupId)
            }
        }
    }
}
