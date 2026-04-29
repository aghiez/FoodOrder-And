package com.example.foodorderapp.ui.buyer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.model.OrderStatus
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.ItemOrderBinding
import com.example.foodorderapp.utils.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit,
    private val onWriteReviewClick: (Order) -> Unit = {}
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            // Order ID
            val shortId = order.orderId.takeLast(8).uppercase()
            binding.tvOrderId.text = "Order #$shortId"

            // Date & time
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
            val date = Date(order.createdAt)
            binding.tvOrderDate.text = "${dateFormat.format(date)} • ${timeFormat.format(date)}"

            // Status badge
            binding.tvOrderStatus.apply {
                text = OrderRepository.getStatusLabel(order.status)
                setBackgroundResource(OrderRepository.getStatusDrawable(order.status))
                setTextColor(OrderRepository.getStatusTextColor(order.status))
            }

            // Items summary
            val itemsText = order.items.joinToString(", ") {
                "${it.quantity}x ${it.foodName}"
            }
            binding.tvOrderItems.text = itemsText

            // Total
            binding.tvOrderTotal.text = Formatter.toRupiah(order.totalAmount)

            // Show Write Review button kalau DELIVERED dan belum direview
            val canReview = order.status == OrderStatus.DELIVERED && !order.isReviewed
            binding.btnWriteReview.visibility = if (canReview) View.VISIBLE else View.GONE
            binding.btnWriteReview.setOnClickListener { onWriteReviewClick(order) }

            // Click whole card → buka detail
            binding.root.setOnClickListener { onOrderClick(order) }
            binding.btnViewDetail.setOnClickListener { onOrderClick(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
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