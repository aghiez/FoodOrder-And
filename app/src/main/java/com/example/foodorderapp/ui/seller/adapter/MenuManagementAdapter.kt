package com.example.foodorderapp.ui.seller.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.databinding.ItemMenuManagementBinding
import com.example.foodorderapp.utils.Formatter

class MenuManagementAdapter(
    private var menus: List<Food>,
    private val onMenuClick: (Food, View) -> Unit
) : RecyclerView.Adapter<MenuManagementAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(
        private val binding: ItemMenuManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(food: Food) {
            val context = binding.root.context

            binding.tvMenuName.text = food.name
            binding.tvMenuPrice.text = Formatter.toRupiah(food.price)
            binding.tvMenuStock.text = context.getString(
                R.string.menu_mgmt_stock_format, food.stock)

            // Image dengan Glide
            Glide.with(context)
                .load(food.imageUrl)
                .placeholder(R.drawable.ic_food)
                .error(R.drawable.ic_food)
                .into(binding.ivMenuImage)

            // Status badge
            if (food.isAvailable) {
                binding.tvMenuStatus.text = context.getString(R.string.menu_mgmt_status_available)
                binding.tvMenuStatus.setBackgroundResource(R.drawable.bg_status_available)
                binding.tvMenuStatus.setTextColor(Color.parseColor("#155724"))
            } else {
                binding.tvMenuStatus.text = context.getString(R.string.menu_mgmt_status_unavailable)
                binding.tvMenuStatus.setBackgroundResource(R.drawable.bg_status_unavailable)
                binding.tvMenuStatus.setTextColor(Color.parseColor("#721C24"))
            }

            // Visual cue: dim items yang unavailable
            binding.root.alpha = if (food.isAvailable) 1.0f else 0.6f

            // Click listeners
            binding.btnMore.setOnClickListener { onMenuClick(food, it) }
            binding.root.setOnClickListener { onMenuClick(food, binding.btnMore) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuManagementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menus[position])
    }

    override fun getItemCount(): Int = menus.size

    fun updateMenus(newMenus: List<Food>) {
        menus = newMenus
        notifyDataSetChanged()
    }
}