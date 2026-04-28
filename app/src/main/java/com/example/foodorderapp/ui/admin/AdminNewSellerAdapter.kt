package com.example.foodorderapp.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.databinding.ItemAdminNewSellerBinding
import java.util.concurrent.TimeUnit

class AdminNewSellerAdapter(
    private var sellers: List<Map<String, Any>>,
    private val onSellerClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<AdminNewSellerAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemAdminNewSellerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(seller: Map<String, Any>) {
            val storeName = seller["storeName"] as? String
                ?: seller["name"] as? String ?: "Store"
            val email = seller["email"] as? String ?: ""
            val createdAt = seller["createdAt"] as? Long ?: 0L

            binding.tvSellerName.text = storeName
            binding.tvSellerEmail.text = email
            binding.tvSellerInitial.text = storeName.firstOrNull()?.uppercase() ?: "S"
            binding.tvJoinedAgo.text = getJoinedAgo(createdAt)

            binding.root.setOnClickListener { onSellerClick(seller) }
        }

        private fun getJoinedAgo(timestamp: Long): String {
            if (timestamp == 0L) return "Recently"

            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.HOURS.toMillis(1) -> "Just now"
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours hr ago"
                }
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days day${if (days > 1) "s" else ""} ago"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminNewSellerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sellers[position])
    }

    override fun getItemCount(): Int = sellers.size

    fun updateSellers(newSellers: List<Map<String, Any>>) {
        sellers = newSellers
        notifyDataSetChanged()
    }
}