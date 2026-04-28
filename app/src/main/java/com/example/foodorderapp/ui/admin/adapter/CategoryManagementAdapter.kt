package com.example.foodorderapp.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Category
import com.example.foodorderapp.data.repository.AdminRepository
import com.example.foodorderapp.databinding.ItemCategoryAdminBinding

class CategoryManagementAdapter(
    private var categories: List<Category>,
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryManagementAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemCategoryAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            val context = binding.root.context

            binding.tvOrderNumber.text = category.order.toString()
            binding.tvCategoryName.text = category.name

            // Load menu count
            binding.tvUsedBy.text = "Loading…"
            AdminRepository.countMenusInCategory(
                categoryId = category.id,
                onSuccess = { count ->
                    binding.tvUsedBy.text = if (count == 0) {
                        "Not used yet"
                    } else {
                        context.getString(R.string.category_used_count, count)
                    }
                },
                onFailure = {
                    binding.tvUsedBy.text = "-"
                }
            )

            // 3-dot menu
            binding.btnMenu.setOnClickListener { view ->
                PopupMenu(context, view).apply {
                    menu.add(context.getString(R.string.category_action_edit))
                    menu.add(context.getString(R.string.category_action_delete))

                    setOnMenuItemClickListener { item ->
                        when (item.title) {
                            context.getString(R.string.category_action_edit) -> {
                                onEdit(category)
                                true
                            }
                            context.getString(R.string.category_action_delete) -> {
                                onDelete(category)
                                true
                            }
                            else -> false
                        }
                    }
                    show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}