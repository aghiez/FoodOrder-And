package com.example.foodorderapp.ui.seller.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.model.OrderStatus
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.ItemSellerOrderBinding
import com.example.foodorderapp.utils.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SellerOrderAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit,
    private val onPrimaryAction: (Order) -> Unit,
    private val onReject: (Order) -> Unit
) : RecyclerView.Adapter<SellerOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(
        private val binding: ItemSellerOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val context = binding.root.context

            // Order ID
            val shortId = order.orderId.takeLast(8).uppercase()
            binding.tvOrderId.text = "Order #$shortId"

            // Time (ago + date)
            val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID"))
            val date = Date(order.createdAt)
            binding.tvOrderTime.text = "${getTimeAgo(order.createdAt)} • ${dateFormat.format(date)}"

            // Status badge
            binding.tvOrderStatus.apply {
                text = OrderRepository.getStatusLabel(order.status)
                setBackgroundResource(OrderRepository.getStatusDrawable(order.status))
                setTextColor(OrderRepository.getStatusTextColor(order.status))
            }

            // Buyer info (akan di-load via getBuyerInfo, untuk now pakai placeholder)
            // TODO: optimize dengan caching atau pre-load buyer info
            loadBuyerInfo(order)

            // Address
            binding.tvAddress.text = order.deliveryAddress

            // Items summary
            val itemsText = order.items.joinToString(", ") {
                "${it.quantity}x ${it.foodName}"
            }
            binding.tvOrderItems.text = itemsText

            // Total
            binding.tvOrderTotal.text = Formatter.toRupiah(order.totalAmount)

            // Action buttons (berbeda per status)
            setupActionButtons(order)

            // Click whole card → detail
            binding.root.setOnClickListener { onOrderClick(order) }
        }

        /**
         * Setup action buttons berdasarkan status.
         */
        private fun setupActionButtons(order: Order) {
            val context = binding.root.context

            when (order.status) {
                OrderStatus.PENDING -> {
                    // Tampilkan 2 buttons: Reject + Accept
                    binding.llActionButtons.visibility = View.VISIBLE
                    binding.btnReject.visibility = View.VISIBLE
                    binding.btnPrimaryAction.text = context.getString(R.string.seller_action_accept)
                    binding.btnPrimaryAction.setBackgroundResource(R.drawable.bg_button_success)
                    binding.btnReject.setOnClickListener { onReject(order) }
                    binding.btnPrimaryAction.setOnClickListener { onPrimaryAction(order) }
                }
                OrderStatus.ACCEPTED -> {
                    binding.llActionButtons.visibility = View.VISIBLE
                    binding.btnReject.visibility = View.GONE
                    binding.btnPrimaryAction.text = context.getString(R.string.seller_action_start_preparing)
                    binding.btnPrimaryAction.setBackgroundResource(R.drawable.bg_button_primary)
                    binding.btnPrimaryAction.setOnClickListener { onPrimaryAction(order) }
                }
                OrderStatus.PREPARING -> {
                    binding.llActionButtons.visibility = View.VISIBLE
                    binding.btnReject.visibility = View.GONE
                    binding.btnPrimaryAction.text = context.getString(R.string.seller_action_mark_ready)
                    binding.btnPrimaryAction.setBackgroundResource(R.drawable.bg_button_primary)
                    binding.btnPrimaryAction.setOnClickListener { onPrimaryAction(order) }
                }
                OrderStatus.READY -> {
                    binding.llActionButtons.visibility = View.VISIBLE
                    binding.btnReject.visibility = View.GONE
                    binding.btnPrimaryAction.text = context.getString(R.string.seller_action_mark_delivered)
                    binding.btnPrimaryAction.setBackgroundResource(R.drawable.bg_button_success)
                    binding.btnPrimaryAction.setOnClickListener { onPrimaryAction(order) }
                }
                else -> {
                    // DELIVERED atau CANCELLED → tidak ada action
                    binding.llActionButtons.visibility = View.GONE
                }
            }
        }

        /**
         * Load buyer info dari Firestore (nama buyer).
         */
        private fun loadBuyerInfo(order: Order) {
            // Sementara kita pakai shortId buyer ID
            // Untuk production, fetch dari users collection (bisa di-optimize dengan cache)
            binding.tvBuyerInfo.text = "Buyer ID: ${order.buyerId.takeLast(8)}"

            // Fetch buyer name dari users collection
            com.example.foodorderapp.data.remote.FirebaseHelper.firestore
                .collection(com.example.foodorderapp.data.remote.FirebaseHelper.COLLECTION_USERS)
                .document(order.buyerId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Unknown"
                        val phone = document.getString("phone") ?: ""
                        binding.tvBuyerInfo.text = if (phone.isNotEmpty()) {
                            "$name • $phone"
                        } else {
                            name
                        }
                    }
                }
        }

        /**
         * Format relative time, e.g. "2 mins ago"
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemSellerOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}