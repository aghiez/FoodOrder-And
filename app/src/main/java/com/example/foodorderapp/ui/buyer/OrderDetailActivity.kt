package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.model.OrderItem
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.ActivityOrderDetailBinding
import com.example.foodorderapp.utils.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private var orderId: String = ""
    private var currentOrder: Order? = null

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        if (orderId.isEmpty()) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { showCancelConfirmation() }

        loadOrderDetail()
    }

    private fun loadOrderDetail() {
        showLoading(true)

        OrderRepository.getOrderById(
            orderId = orderId,
            onSuccess = { order ->
                showLoading(false)
                currentOrder = order
                displayOrder(order)
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun displayOrder(order: Order) {
        binding.contentLayout.visibility = View.VISIBLE

        // Order ID & Date
        val shortId = order.orderId.takeLast(8).uppercase()
        binding.tvOrderId.text = "Order #$shortId"

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        val date = Date(order.createdAt)
        binding.tvOrderDate.text = "${dateFormat.format(date)} • ${timeFormat.format(date)}"

        // Status
        binding.tvOrderStatus.apply {
            text = OrderRepository.getStatusLabel(order.status)
            setBackgroundResource(OrderRepository.getStatusDrawable(order.status))
            setTextColor(OrderRepository.getStatusTextColor(order.status))
        }

        // Items
        binding.llItemsContainer.removeAllViews()
        for (item in order.items) {
            val itemView = createItemRow(item)
            binding.llItemsContainer.addView(itemView)
        }

        // Totals
        val subtotal = order.totalAmount - order.deliveryFee
        binding.tvSubtotal.text = Formatter.toRupiah(subtotal)
        binding.tvDeliveryFee.text = Formatter.toRupiah(order.deliveryFee)
        binding.tvTotal.text = Formatter.toRupiah(order.totalAmount)

        // Delivery info
        binding.tvDeliveryAddress.text = order.deliveryAddress
        binding.tvPaymentMethod.text = order.paymentMethod
        binding.tvNotes.text = if (order.notes.isEmpty())
            getString(R.string.order_detail_no_notes) else order.notes

        // Cancel button visibility
        binding.btnCancel.visibility = if (OrderRepository.canBeCancelled(order.status))
            View.VISIBLE else View.GONE
    }

    /**
     * Buat row untuk satu item order secara programatik.
     * Format: 2x Nasi Goreng                 Rp 30.000
     */
    private fun createItemRow(item: OrderItem): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (4 * resources.displayMetrics.density).toInt()
                bottomMargin = (4 * resources.displayMetrics.density).toInt()
            }
        }

        val tvItemName = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "${item.quantity}x ${item.foodName}"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@OrderDetailActivity, R.color.text_primary))
        }

        val tvItemSubtotal = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = Formatter.toRupiah(item.subtotal)
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@OrderDetailActivity, R.color.text_primary))
        }

        container.addView(tvItemName)
        container.addView(tvItemSubtotal)
        return container
    }

    private fun showCancelConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.orders_cancel)
            .setMessage(R.string.order_detail_cancel_confirm)
            .setPositiveButton(R.string.action_yes) { _, _ ->
                cancelOrder()
            }
            .setNegativeButton(R.string.action_no, null)
            .show()
    }

    private fun cancelOrder() {
        showLoading(true)

        OrderRepository.cancelOrder(
            orderId = orderId,
            onSuccess = {
                showLoading(false)
                Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                loadOrderDetail()  // Reload untuk update status
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this, "Failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) binding.contentLayout.visibility = View.GONE
    }
}