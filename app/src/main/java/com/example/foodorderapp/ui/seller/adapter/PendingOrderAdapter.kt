package com.example.foodorderapp.ui.seller.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.databinding.ItemPendingOrderBinding
import com.example.foodorderapp.utils.Formatter
import java.util.concurrent.TimeUnit

class PendingOrderAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<PendingOrderAdapter.PendingOrderViewHolder>() {

    inner class PendingOrderViewHolder(
        private val binding: ItemPendingOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            // Order ID (8 karakter terakhir)
            val shortId = order.orderId.takeLast(8).uppercase()
            binding.tvOrderId.text = "Order #$shortId"

            // Time ago
            binding.tvOrderTime.text = getTimeAgo(order.createdAt)

            // Items summary
            val itemsText = order.items.joinToString(", ") {
                "${it.quantity}x ${it.foodName}"
            }
            binding.tvOrderItems.text = itemsText

            // Total
            binding.tvOrderTotal.text = Formatter.toRupiah(order.totalAmount)

            // Click listeners
            binding.btnViewDetail.setOnClickListener { onOrderClick(order) }
            binding.root.setOnClickListener { onOrderClick(order) }
        }

        /**
         * Format relative time, contoh: "2 mins ago", "1 hour ago"
         */
        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$mins min${if (mins > 1) "s" else ""} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours hour${if (hours > 1) "s" else ""} ago"
                }
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days day${if (days > 1) "s" else ""} ago"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingOrderViewHolder {
        val binding = ItemPendingOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return PendingOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingOrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}