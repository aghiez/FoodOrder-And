package com.example.foodorderapp.ui.buyer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Category
import com.example.foodorderapp.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0  // default: kategori pertama (All)

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category, position: Int) {
            val context = binding.root.context
            binding.tvCategoryName.text = category.name

            // Highlight kategori yang dipilih
            if (position == selectedPosition) {
                binding.tvCategoryName.setBackgroundResource(R.drawable.bg_category_selected)
                binding.tvCategoryName.setTextColor(
                    ContextCompat.getColor(context, R.color.white))
            } else {
                binding.tvCategoryName.setBackgroundResource(R.drawable.bg_category_unselected)
                binding.tvCategoryName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary))
            }

            binding.root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onCategoryClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount(): Int = categories.size
}