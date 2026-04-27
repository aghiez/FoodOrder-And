package com.example.foodorderapp.ui.buyer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.databinding.ItemFoodBinding
import com.example.foodorderapp.utils.Formatter

class FoodAdapter(
    private var foods: List<Food>,
    private val onFoodClick: (Food) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(
        private val binding: ItemFoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(food: Food) {
            binding.tvFoodName.text = food.name
            binding.tvFoodDescription.text = food.description
            binding.tvFoodPrice.text = Formatter.toRupiah(food.price)
            binding.tvFoodRating.text = food.rating.toString()
            binding.tvFoodSold.text = "• ${food.totalSold} sold"

            // Load image dengan Glide
            Glide.with(binding.root.context)
                .load(food.imageUrl)
                .placeholder(R.drawable.ic_food)
                .error(R.drawable.ic_food)
                .into(binding.ivFoodImage)

            // Click listener
            binding.root.setOnClickListener {
                onFoodClick(food)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(foods[position])
    }

    override fun getItemCount(): Int = foods.size

    /**
     * Update list food dengan data baru.
     */
    fun updateFoods(newFoods: List<Food>) {
        foods = newFoods
        notifyDataSetChanged()
    }
}