package com.example.foodorderapp.ui.buyer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.ItemOrderBinding
import com.example.foodorderapp.utils.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            // Order ID (tampilkan 8 karakter terakhir untuk readability)
            val shortId = order.orderId.takeLast(8).uppercase()
            binding.tvOrderId.text = "Order #$shortId"

            // Date & time
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
            val date = Date(order.createdAt)
            binding.tvOrderDate.text = "${dateFormat.format(date)} • ${timeFormat.format(date)}"

            // Status badge dengan warna sesuai status
            // Akan otomatis support SHIPPED karena getStatusLabel/Drawable/TextColor
            // sudah di-update di OrderRepository
            binding.tvOrderStatus.apply {
                text = OrderRepository.getStatusLabel(order.status)
                setBackgroundResource(OrderRepository.getStatusDrawable(order.status))
                setTextColor(OrderRepository.getStatusTextColor(order.status))
            }

            // Items summary (contoh: "2x Nasi Goreng, 1x Es Teh Manis")
            val itemsText = order.items.joinToString(", ") {
                "${it.quantity}x ${it.foodName}"
            }
            binding.tvOrderItems.text = itemsText

            // Total amount
            binding.tvOrderTotal.text = Formatter.toRupiah(order.totalAmount)

            // Click whole card → buka detail
            binding.root.setOnClickListener { onOrderClick(order) }

            // Click button "View Detail" → buka detail
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

    /**
     * Update list orders dan refresh adapter.
     */
    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}