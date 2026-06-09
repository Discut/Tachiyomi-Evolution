package eu.kanade.tachiyomi.ui.reader.sheet

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ItemAiTagResultBinding
import eu.kanade.tachiyomi.util.system.toast
import timber.log.Timber

class AITagResultAdapter(
    val tagClickedListener: (result: AIPredictResult) -> Unit,
    val tagLongClickListener: ((result: AIPredictResult, view: View) -> Unit)? = null,
) : RecyclerView.Adapter<AITagResultAdapter.VH>() {

    private var results: List<AIPredictResult> = emptyList()

    fun submitList(newResults: List<AIPredictResult>) {
        val diff = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize() = results.size
                override fun getNewListSize() = newResults.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                    results[oldPos].tagName == newResults[newPos].tagName
                override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                    results[oldPos] == newResults[newPos]
            },
        )
        results = newResults
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ItemAiTagResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val result = results[position]
        val ctx = holder.binding.root.context

        // Suffix for filtered tags
        val suffix = if (result.isFiltered) ctx.getString(R.string.ai_tag_filtered_suffix) else ""
        holder.binding.tagName.text = result.tagName + suffix

        holder.binding.confidenceText.text = "${(result.score * 100).toInt()}%"

        // Progress bar width
        holder.binding.root.post {
            val parentWidth = holder.binding.root.width
            if (parentWidth > 0) {
                val fillWidth = (parentWidth * result.score).toInt()
                (holder.binding.progressFill.layoutParams as? FrameLayout.LayoutParams)?.apply {
                    width = fillWidth
                }
                holder.binding.progressFill.requestLayout()
            }
        }

        // Visual states: background color = adoption, suffix = filter
        if (result.isAdopted) {
            holder.binding.progressFill.setBackgroundColor(0x33888888.toInt())
            holder.binding.root.alpha = 0.4f
            holder.binding.root.isClickable = false
        } else {
            holder.binding.progressFill.setBackgroundColor(0x3300BFA5.toInt())
            holder.binding.root.alpha = if (result.isFiltered) 0.6f else 1f
            holder.binding.root.isClickable = true
            holder.binding.root.setOnClickListener {
                onAITagClicked(result, holder.binding)
            }
        }

        // Long-click: show filter/unfilter ActionMode (adopted+unfiltered excluded)
        val canLongPress = !result.isAdopted || result.isFiltered
        holder.binding.root.isLongClickable = canLongPress
        if (canLongPress) {
            holder.binding.root.setOnLongClickListener { view ->
                tagLongClickListener?.invoke(result, view)
                true
            }
        } else {
            holder.binding.root.setOnLongClickListener(null)
        }
    }

    override fun getItemCount(): Int = results.size

    private fun onAITagClicked(result: AIPredictResult, itemBinding: ItemAiTagResultBinding) {
        try {
            tagClickedListener.invoke(result)
            itemBinding.progressFill.setBackgroundColor(0x33888888.toInt())
            itemBinding.root.alpha = 0.4f
            itemBinding.root.isClickable = false
        } catch (e: Exception) {
            Timber.e(e, "采纳AI标签失败")
            itemBinding.root.context.toast("采纳失败: ${e.message}")
        }
    }

    class VH(val binding: ItemAiTagResultBinding) : RecyclerView.ViewHolder(binding.root)
}
