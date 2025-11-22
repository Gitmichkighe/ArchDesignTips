package com.example.appdir

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appdir.databinding.ItemRuleBinding
import com.example.appdir.model.ListItem
import com.example.appdir.model.Rule

class RulesAdapter(
    private val onToggle: (Rule) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ListItem>()

    fun submitList(list: List<ListItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): ListItem = items[position]

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItem.Header -> 0
        is ListItem.RuleItem -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val textView = TextView(parent.context).apply {
                setPadding(32, 24, 32, 24)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            }
            HeaderViewHolder(textView)
        } else {
            val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            RuleViewHolder(binding, onToggle)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ListItem.RuleItem -> (holder as RuleViewHolder).bind(item.rule)
        }
    }

    class HeaderViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(item: ListItem.Header) { textView.text = item.category }
    }

    inner class RuleViewHolder(
        private val binding: ItemRuleBinding,
        private val onToggle: (Rule) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rule: Rule) {
            binding.tvRule.text = rule.text
            binding.ivStar.setImageResource(
                if (rule.isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            binding.ivStar.setOnClickListener {
                rule.isFavorite = !rule.isFavorite
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    this@RulesAdapter.notifyItemChanged(position)
                }
                onToggle(rule)
            }
        }

    }
}
