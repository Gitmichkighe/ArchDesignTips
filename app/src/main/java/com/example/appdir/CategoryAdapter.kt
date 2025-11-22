package com.example.appdir

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appdir.model.Category

class CategoryAdapter(
    private val onCategoryClicked: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiff) {

    /** Returns category by name */
    fun getCategoryByName(name: String): Category? =
        currentList.find { it.category == name }

    /** Increment ads watched for a category; unlock if completed */
    fun incrementAdsWatchedForCategory(name: String): Boolean {
        val category = getCategoryByName(name) ?: return false
        category.adsWatched++

        val unlocked = category.adsWatched >= category.adsToUnlock
        if (unlocked) category.locked = false

        notifyDataSetChanged()
        return unlocked
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvInitial: TextView = itemView.findViewById(R.id.tvInitial)
        private val ivLock: ImageView = itemView.findViewById(R.id.ivLock)
        private val tvUnlock: TextView = itemView.findViewById(R.id.tvFreeUnlock)

        fun bind(category: Category) {
            val context = itemView.context

            // Category name
            tvCategory.text = category.category

            // Placeholder initial
            val initial = category.category.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            tvInitial.text = initial

            // Lookup icon drawable
            val iconName = category.category.lowercase().replace(" ", "_")
            val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)

            if (resId != 0) {
                // Icon exists: show icon, hide initial
                ivIcon.setImageResource(resId)
                ivIcon.visibility = View.VISIBLE
                tvInitial.visibility = View.GONE
            } else {
                // No icon: show initial on blue circle
                ivIcon.visibility = View.GONE
                tvInitial.visibility = View.VISIBLE
            }

            // Lock state
            if (category.locked) {
                ivLock.visibility = View.VISIBLE
                tvUnlock.visibility = View.VISIBLE
                tvUnlock.text = "Free unlock:\nView ${category.adsToUnlock} ads"
            } else {
                ivLock.visibility = View.GONE
                tvUnlock.visibility = View.GONE
            }

            // Click listener
            itemView.setOnClickListener { onCategoryClicked(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CategoryViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
        )

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) =
        holder.bind(getItem(position))

    object CategoryDiff : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category) =
            oldItem.category == newItem.category

        override fun areContentsTheSame(oldItem: Category, newItem: Category) =
            oldItem == newItem
    }
}
