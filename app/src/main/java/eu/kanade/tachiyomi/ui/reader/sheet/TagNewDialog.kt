package eu.kanade.tachiyomi.ui.reader.sheet

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.data.database_orm.GalleryDatabase
import eu.kanade.tachiyomi.data.gallery.GalleryManager
import eu.kanade.tachiyomi.databinding.TagNewDialogBinding
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.gallery.local.LocalGallerySource
import eu.kanade.tachiyomi.util.generateTimestampBasedID
import uy.kohesive.injekt.injectLazy

class TagNewDialog {

    val sourceManager by injectLazy<SourceManager>()

    val galleryManager by injectLazy<GalleryManager>()

    val room by injectLazy<GalleryDatabase>()

    private var selectedSource: Long = 0
    private var selectedType: Long = 0

    suspend fun show(
        activity: Activity,
        callback: (TagVo) -> Unit,
    ) {
        val inflate = TagNewDialogBinding.inflate(activity.layoutInflater)

        val sources = sourceManager.getAllGallerySources().map {
            Item(it.name, it.id)
        }

        val tagTypes = room.getTagTypeDao().getAll().map {
            Item(it.typeName, it.typeId)
        }

        inflate.tagSourceInput.setAdapter(InnerArrayAdapter(sources))
        if (sources.isNotEmpty()) {
            val default = sources.find { it.id == LocalGallerySource.ID } ?: sources[0]
            selectedSource = default.id
            inflate.tagSourceInput.setText(default.label, false)
        }
        inflate.tagSourceInput.setOnItemClickListener { _, _, position, _ ->
            selectedSource = sources[position].id
        }

        inflate.tagTypeInput.setAdapter(InnerArrayAdapter(tagTypes))
        if (tagTypes.isNotEmpty()) {
            val default = tagTypes.find { it.label == "other" } ?: tagTypes[0]
            inflate.tagTypeInput.setText(default.label, false)
            selectedType = default.id
        }
        inflate.tagTypeInput.setOnItemClickListener { _, _, position, _ ->
            selectedType = tagTypes[position].id
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Create Tag")
            .setView(inflate.root)
            .setPositiveButton("Save") { _, _ ->
                val tagName = inflate.tagName.editText?.text?.toString() ?: return@setPositiveButton
                TagVo(
                    tagId = generateTimestampBasedID(),
                    tagValue = tagName,
                    typeId = selectedType,
                    source = selectedSource,
                    isSelected = false,
                ).apply(callback)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class InnerArrayAdapter(
        private var data: List<Item>,
    ) : BaseAdapter(), Filterable {
        private val mFilter by lazy {
            ArrayFilter(this)
        }

        override fun getCount(): Int {
            return data.size
        }

        override fun getItem(position: Int): Any {
            return data[position].label
        }

        override fun getItemId(position: Int): Long {
            return data[position].id
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return TextView(parent?.context!!).apply {
                setPadding(16, 24, 16, 24)
                val item = data[position]
                text = item.label
                tag = item.id
            }
        }

        override fun getFilter(): Filter {
            return mFilter
        }

        private class ArrayFilter(val innerArrayAdapter: InnerArrayAdapter) : Filter() {
            var mOriginalValues = emptyList<Item>()
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                mOriginalValues = ArrayList(innerArrayAdapter.data)

                val result = mOriginalValues.filter {
                    it.label.uppercase().contains(constraint ?: "")
                }

                return filterResults.apply {
                    count = result.size
                    values = result
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val list = (results?.values as? List<Item>) ?: emptyList<Item>()
                innerArrayAdapter.data = list
                if (list.isEmpty()) {
                    innerArrayAdapter.notifyDataSetInvalidated()
                } else {
                    innerArrayAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    data class Item(
        val label: String,
        val id: Long,
    )
}
