package eu.kanade.tachiyomi.ui.gallery

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.chip.Chip
import eu.kanade.tachiyomi.R

/**
 * 图片标签操作浮层
 */
class FloatingImageTagActionModeCallback(
    private val chip: Chip?,
) : android.view.ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(
            R.menu.image_tag_actions,
            menu,
        )
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_copy -> {
            }

            R.id.action_local_search -> {
            }

            R.id.action_delete -> {
            }
        }
        mode?.finish()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        TODO("Not yet implemented")
    }
}
