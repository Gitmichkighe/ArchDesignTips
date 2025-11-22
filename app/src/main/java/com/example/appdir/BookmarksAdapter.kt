package com.example.appdir

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appdir.databinding.ItemBookmarkBinding
import org.json.JSONObject

class BookmarksAdapter(
    private val onClick: (JSONObject) -> Unit,
    private val onLongClick: (JSONObject) -> Unit
) : RecyclerView.Adapter<BookmarksAdapter.Holder>() {

    private var data: List<JSONObject> = emptyList()
    private var selectedItem: JSONObject? = null

    fun submitList(list: List<JSONObject>) {
        data = list
        notifyDataSetChanged()
    }

    fun setSelectedItem(bookmark: JSONObject?) {
        selectedItem = bookmark
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemBookmarkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = data[position]

        holder.binding.tvBookmarkName.text = item.getString("name")
        holder.binding.tvBookmarkDetails.text = "${item.getString("category")} - Page ${item.getInt("page") + 1}"

        // Highlight selected item
        holder.binding.root.isSelected = item == selectedItem
        holder.binding.root.setBackgroundColor(
            if (item == selectedItem) holder.binding.root.context.getColor(android.R.color.darker_gray)
            else holder.binding.root.context.getColor(android.R.color.transparent)
        )

        holder.binding.root.setOnClickListener { onClick(item) }
        holder.binding.root.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = data.size
}
