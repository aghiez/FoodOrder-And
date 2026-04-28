package com.example.foodorderapp.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.ItemAdminRecentOrderBinding
import com.example.foodorderapp.utils.Formatter
import java.util.concurrent.TimeUnit

class AdminRecentOrderAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<AdminRecentOrderAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemAdminRecentOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            // Order ID
            val shortId = order.orderId.takeLast(8).uppercase()
            binding.tvOrderId.text = "Order #$shortId"

            // Time ago
            binding.tvOrderTime.text = getTimeAgo(order.createdAt)

            // Status badge
            binding.tvOrderStatus.apply {
                text = OrderRepository.getStatusLabel(order.status)
                setBackgroundResource(OrderRepository.getStatusDrawable(order.status))
                setTextColor(OrderRepository.getStatusTextColor(order.status))
            }

            // Total
            binding.tvOrderTotal.text = Formatter.toRupiah(order.totalAmount)

            // Buyer name (load async)
            binding.tvBuyerName.text = "By: Loading..."
            FirebaseHelper.firestore
                .collection(FirebaseHelper.COLLECTION_USERS)
                .document(order.buyerId)
                .get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "Unknown"
                    binding.tvBuyerName.text = "By: $name"
                }
                .addOnFailureListener {
                    binding.tvBuyerName.text = "By: Unknown"
                }

            binding.root.setOnClickListener { onOrderClick(order) }
        }

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminRecentOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}