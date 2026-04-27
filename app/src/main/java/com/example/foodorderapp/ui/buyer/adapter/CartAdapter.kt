package com.example.foodorderapp.ui.buyer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.CartItem
import com.example.foodorderapp.databinding.ItemCartBinding
import com.example.foodorderapp.utils.Formatter

class CartAdapter(
    private var items: List<CartItem>,
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.tvCartItemName.text = item.foodName
            binding.tvCartItemPrice.text = Formatter.toRupiah(item.price)
            binding.tvCartItemQuantity.text = item.quantity.toString()
            binding.tvCartItemSubtotal.text = "Subtotal: ${Formatter.toRupiah(item.subtotal)}"

            Glide.with(binding.root.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_food)
                .error(R.drawable.ic_food)
                .into(binding.ivCartItemImage)

            binding.btnIncrease.setOnClickListener { onIncrease(item) }
            binding.btnDecrease.setOnClickListener { onDecrease(item) }
            binding.btnRemove.setOnClickListener { onRemove(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}