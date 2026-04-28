package com.example.foodorderapp.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.ItemAdminOrderBinding
import com.example.foodorderapp.utils.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdminOrderAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

    // Cache buyer & seller names untuk avoid re-fetch
    private val nameCache = mutableMapOf<String, String>()

    inner class ViewHolder(
        private val binding: ItemAdminOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            // Order ID
            val shortId = order.orderId.takeLast(8).uppercase()
            binding.tvOrderId.text = "Order #$shortId"

            // Time
            val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID"))
            val date = Date(order.createdAt)
            binding.tvOrderTime.text = "${getTimeAgo(order.createdAt)} • ${dateFormat.format(date)}"

            // Status badge
            binding.tvOrderStatus.apply {
                text = OrderRepository.getStatusLabel(order.status)
                setBackgroundResource(OrderRepository.getStatusDrawable(order.status))
                setTextColor(OrderRepository.getStatusTextColor(order.status))
            }

            // Items
            val itemsText = order.items.joinToString(", ") {
                "${it.quantity}x ${it.foodName}"
            }
            binding.tvOrderItems.text = itemsText

            // Total
            binding.tvOrderTotal.text = Formatter.toRupiah(order.totalAmount)

            // Load buyer & seller names
            loadName(order.buyerId, "buyer") { name ->
                if (binding.tvBuyerName.tag == order.orderId) {
                    binding.tvBuyerName.text = name
                }
            }

            loadName(order.sellerId, "seller") { name ->
                if (binding.tvSellerName.tag == order.orderId) {
                    binding.tvSellerName.text = name
                }
            }

            // Tag untuk verify saat callback (cegah race condition di RecyclerView)
            binding.tvBuyerName.tag = order.orderId
            binding.tvSellerName.tag = order.orderId
            binding.tvBuyerName.text = "Loading…"
            binding.tvSellerName.text = "Loading…"

            // Click
            binding.root.setOnClickListener { onOrderClick(order) }
        }

        /**
         * Load name dengan cache untuk efisiensi.
         */
        private fun loadName(userId: String, type: String, onResult: (String) -> Unit) {
            // Cek cache dulu
            nameCache[userId]?.let {
                onResult(it)
                return
            }

            FirebaseHelper.firestore
                .collection(FirebaseHelper.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    val name = if (type == "seller") {
                        doc.getString("storeName")
                            ?: doc.getString("name") ?: "Unknown"
                    } else {
                        doc.getString("name") ?: "Unknown"
                    }
                    nameCache[userId] = name
                    onResult(name)
                }
                .addOnFailureListener {
                    onResult("Unknown")
                }
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
        val binding = ItemAdminOrderBinding.inflate(
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