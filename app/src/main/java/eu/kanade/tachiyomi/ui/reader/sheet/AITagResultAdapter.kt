package eu.kanade.tachiyomi.ui.reader.sheet

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.databinding.ItemAiTagResultBinding
import eu.kanade.tachiyomi.util.system.toast
import timber.log.Timber

class AITagResultAdapter(
    val tagClickedListener: (result: AIPredictResult) -> Unit,
) : RecyclerView.Adapter<AITagResultAdapter.VH>() {

    private var results: List<AIPredictResult> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newResults: List<AIPredictResult>) {
        results = newResults
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ItemAiTagResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val result = results[position]

        holder.binding.tagName.text = result.tagName
        holder.binding.confidenceText.text = "${(result.score * 100).toInt()}%"

        // 设置进度条宽度（需要等布局完成后获取宽度）
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

        if (result.isAdopted) {
            holder.binding.progressFill.setBackgroundColor(0x33888888)
            holder.binding.root.alpha = 0.4f
            holder.binding.root.isClickable = false
        } else {
            holder.binding.progressFill.setBackgroundColor(0x3300BFA5)
            holder.binding.root.alpha = 1f
            holder.binding.root.isClickable = true
            holder.binding.root.setOnClickListener {
                onAITagClicked(result, holder.binding)
            }
        }
    }

    override fun getItemCount(): Int = results.size

    private fun onAITagClicked(result: AIPredictResult, itemBinding: ItemAiTagResultBinding) {
        try {
            tagClickedListener.invoke(result)
            itemBinding.progressFill.setBackgroundColor(0x33888888)
            itemBinding.root.alpha = 0.4f
            itemBinding.root.isClickable = false
        } catch (e: Exception) {
            Timber.e(e, "采纳AI标签失败")
            itemBinding.root.context.toast("采纳失败: ${e.message}")
        }
    }

    class VH(val binding: ItemAiTagResultBinding) : RecyclerView.ViewHolder(binding.root)
}
