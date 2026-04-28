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
import com.example.foodorderapp.data.model.OrderStatus
import com.example.foodorderapp.data.remote.FirebaseHelper
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

    // Mode: "buyer" or "seller" (default: buyer)
    private var viewMode: String = MODE_BUYER

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_VIEW_MODE = "extra_view_mode"
        const val MODE_BUYER = "buyer"
        const val MODE_SELLER = "seller"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        viewMode = intent.getStringExtra(EXTRA_VIEW_MODE) ?: MODE_BUYER

        if (orderId.isEmpty()) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { handleCancelButtonClick() }

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

        // Configure UI based on mode
        if (viewMode == MODE_SELLER) {
            configureSellerMode(order)
        } else {
            configureBuyerMode(order)
        }
    }

    /**
     * Buyer mode: tombol Cancel Order kalau status memungkinkan.
     */
    private fun configureBuyerMode(order: Order) {
        when (order.status) {
            // Status pending atau accepted: bisa cancel
            OrderStatus.PENDING, OrderStatus.ACCEPTED -> {
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.text = getString(R.string.orders_cancel)
                binding.btnCancel.setBackgroundResource(R.drawable.bg_cancel_button)
                binding.btnCancel.setTextColor(ContextCompat.getColor(this, R.color.error))
            }

            // Status SHIPPED: tombol Confirm Received
            OrderStatus.SHIPPED -> {
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.text = getString(R.string.buyer_action_confirm_received)
                binding.btnCancel.setBackgroundResource(R.drawable.bg_button_success)
                binding.btnCancel.setTextColor(ContextCompat.getColor(this, R.color.white))
            }

            // Status lain: tidak ada tombol
            else -> {
                binding.btnCancel.visibility = View.GONE
            }
        }
    }

    /**
     * Seller mode: tampilkan info buyer & tombol update status.
     */
    private fun configureSellerMode(order: Order) {
        // Hide button cancel default karena kita ganti dengan dynamic action
        binding.btnCancel.visibility = View.GONE

        // Load buyer info
        loadBuyerInfo(order.buyerId)

        // Show action button sesuai status
        showSellerActionButton(order)
    }

    /**
     * Load nama buyer dari Firestore.
     */
    private fun loadBuyerInfo(buyerId: String) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(buyerId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "Unknown"
                    val phone = document.getString("phone") ?: ""

                    // Tambahkan info buyer di atas card delivery info
                    addBuyerInfoCard(name, phone)
                }
            }
    }

    /**
     * Tambahkan card info buyer di atas card delivery info.
     */
    private fun addBuyerInfoCard(name: String, phone: String) {
        // Cari container utama (LinearLayout di dalam ScrollView)
        val container = binding.contentLayout

        // Cari index card pertama (delivery info card)
        // Kita tambahkan card buyer sebelum tombol cancel

        val buyerCard = androidx.cardview.widget.CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
            radius = 12 * resources.displayMetrics.density
            cardElevation = 2 * resources.displayMetrics.density
            setCardBackgroundColor(ContextCompat.getColor(
                this@OrderDetailActivity, R.color.white))
        }

        val cardContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt()
            )
        }

        // Title
        val tvTitle = TextView(this).apply {
            text = getString(R.string.seller_detail_buyer_info)
            textSize = 14f
            setTextColor(ContextCompat.getColor(
                this@OrderDetailActivity, R.color.text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        cardContent.addView(tvTitle)

        // Name label
        val tvNameLabel = TextView(this).apply {
            text = getString(R.string.seller_detail_buyer_name)
            textSize = 12f
            setTextColor(ContextCompat.getColor(
                this@OrderDetailActivity, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }
        cardContent.addView(tvNameLabel)

        // Name value
        val tvName = TextView(this).apply {
            text = name
            textSize = 14f
            setTextColor(ContextCompat.getColor(
                this@OrderDetailActivity, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (4 * resources.displayMetrics.density).toInt()
            }
        }
        cardContent.addView(tvName)

        // Phone (kalau ada)
        if (phone.isNotEmpty()) {
            val tvPhoneLabel = TextView(this).apply {
                text = getString(R.string.seller_detail_buyer_phone)
                textSize = 12f
                setTextColor(ContextCompat.getColor(
                    this@OrderDetailActivity, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (10 * resources.displayMetrics.density).toInt()
                }
            }
            cardContent.addView(tvPhoneLabel)

            val tvPhone = TextView(this).apply {
                text = phone
                textSize = 14f
                setTextColor(ContextCompat.getColor(
                    this@OrderDetailActivity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (4 * resources.displayMetrics.density).toInt()
                }
            }
            cardContent.addView(tvPhone)
        }

        buyerCard.addView(cardContent)

        // Insert di posisi index 0 (paling atas content)
        container.addView(buyerCard, 0)
    }

    /**
     * Tampilkan action button sesuai status order.
     */
    private fun showSellerActionButton(order: Order) {
        when (order.status) {
            OrderStatus.PENDING -> {
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.text = getString(R.string.seller_action_accept)
                binding.btnCancel.setBackgroundResource(R.drawable.bg_button_success)
                binding.btnCancel.setTextColor(ContextCompat.getColor(this, R.color.white))

                addRejectButton(order)
            }
            OrderStatus.ACCEPTED -> {
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.text = getString(R.string.seller_action_start_preparing)
                binding.btnCancel.setBackgroundResource(R.drawable.bg_button_primary)
                binding.btnCancel.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            OrderStatus.PREPARING -> {
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.text = getString(R.string.seller_action_mark_ready)
                binding.btnCancel.setBackgroundResource(R.drawable.bg_button_primary)
                binding.btnCancel.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            OrderStatus.READY -> {
                // Yang berubah
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.text = getString(R.string.seller_action_mark_shipped)
                binding.btnCancel.setBackgroundResource(R.drawable.bg_button_success)
                binding.btnCancel.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            OrderStatus.SHIPPED -> {
                // Tidak ada action, tampilkan info text
                binding.btnCancel.visibility = View.GONE
                addShippedInfo()
            }
            OrderStatus.DELIVERED, OrderStatus.CANCELLED -> {
                binding.btnCancel.visibility = View.GONE
                addCompletedInfo(order)
            }
        }
    }

    /**
     * Tambah info text untuk seller saat status SHIPPED.
     */
    private fun addShippedInfo() {
        val container = binding.contentLayout

        val tvInfo = TextView(this).apply {
            text = "Order has been shipped. Waiting for buyer confirmation..."
            textSize = 13f
            setTextColor(ContextCompat.getColor(
                this@OrderDetailActivity, R.color.text_secondary))
            gravity = android.view.Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
                bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }
        }

        container.addView(tvInfo)
    }

    /**
     * Tambah tombol Reject (untuk pending order di seller mode).
     */
    private fun addRejectButton(order: Order) {
        val container = binding.contentLayout

        val btnReject = androidx.appcompat.widget.AppCompatButton(this).apply {
            text = getString(R.string.seller_action_reject)
            setBackgroundResource(R.drawable.bg_button_outline)
            setTextColor(ContextCompat.getColor(this@OrderDetailActivity, R.color.error))
            isAllCaps = false
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * resources.displayMetrics.density).toInt()
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
                bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }

            setOnClickListener { showRejectConfirmation(order) }
        }

        // Tambah setelah btnCancel (Accept button untuk pending)
        val cancelIndex = container.indexOfChild(binding.btnCancel)
        container.addView(btnReject, cancelIndex + 1)
    }

    /**
     * Tambah info text untuk order yang sudah completed/cancelled.
     */
    private fun addCompletedInfo(order: Order) {
        val container = binding.contentLayout

        val infoText = if (order.status == OrderStatus.DELIVERED) {
            getString(R.string.seller_detail_completed_info)
        } else {
            getString(R.string.seller_detail_cancelled_info)
        }

        val tvInfo = TextView(this).apply {
            text = infoText
            textSize = 13f
            setTextColor(ContextCompat.getColor(
                this@OrderDetailActivity, R.color.text_secondary))
            gravity = android.view.Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
                bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }
        }

        container.addView(tvInfo)
    }

    /**
     * Handle click tombol Cancel (yang sebenarnya jadi multi-purpose action di seller mode).
     */
    private fun handleCancelButtonClick() {
        val order = currentOrder ?: return

        if (viewMode == MODE_BUYER) {
            // Buyer mode: action tergantung status
            when (order.status) {
                OrderStatus.PENDING, OrderStatus.ACCEPTED -> showCancelConfirmation()
                OrderStatus.SHIPPED -> showConfirmReceivedDialog()
                else -> { /* shouldn't reach here */ }
            }
        } else {
            // Seller mode: action sesuai status
            handleSellerAction(order)
        }
    }

    /**
     * Dialog konfirmasi: buyer yakin sudah terima order?
     */
    private fun showConfirmReceivedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.buyer_confirm_received_title)
            .setMessage(R.string.buyer_confirm_received_message)
            .setPositiveButton(R.string.buyer_action_yes_received) { _, _ ->
                confirmReceived()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Buyer confirm bahwa pesanan sudah diterima.
     */
    private fun confirmReceived() {
        showLoading(true)

        OrderRepository.confirmReceived(
            orderId = orderId,
            onSuccess = {
                Toast.makeText(this, getString(R.string.buyer_order_confirmed),
                    Toast.LENGTH_SHORT).show()
                loadOrderDetail()  // Reload page
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this,
                    getString(R.string.buyer_order_confirm_failed) + ": $errorMessage",
                    Toast.LENGTH_LONG).show()
            }
        )
    }

    /**
     * Handle action button di seller mode.
     */
    private fun handleSellerAction(order: Order) {
        when (order.status) {
            OrderStatus.PENDING -> showAcceptConfirmation(order)
            OrderStatus.ACCEPTED -> updateStatus(order, OrderStatus.PREPARING,
                getString(R.string.seller_order_preparing))
            OrderStatus.PREPARING -> updateStatus(order, OrderStatus.READY,
                getString(R.string.seller_order_ready))
            OrderStatus.READY -> updateStatus(order, OrderStatus.SHIPPED,
                getString(R.string.seller_order_shipped))
        }
    }

    private fun showAcceptConfirmation(order: Order) {
        AlertDialog.Builder(this)
            .setTitle(R.string.seller_confirm_accept_title)
            .setMessage(R.string.seller_confirm_accept_message)
            .setPositiveButton(R.string.seller_action_accept) { _, _ ->
                updateStatus(order, OrderStatus.ACCEPTED,
                    getString(R.string.seller_order_accepted))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showRejectConfirmation(order: Order) {
        AlertDialog.Builder(this)
            .setTitle(R.string.seller_confirm_reject_title)
            .setMessage(R.string.seller_confirm_reject_message)
            .setPositiveButton(R.string.seller_action_reject) { _, _ ->
                updateStatus(order, OrderStatus.CANCELLED,
                    getString(R.string.seller_order_rejected))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun updateStatus(order: Order, newStatus: String, successMessage: String) {
        showLoading(true)

        OrderRepository.updateOrderStatus(
            orderId = order.orderId,
            newStatus = newStatus,
            onSuccess = {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                loadOrderDetail()  // Reload untuk update tampilan
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this,
                    "Failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    /**
     * Buyer mode: cancel order.
     */
    private fun showCancelConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.orders_cancel)
            .setMessage(R.string.order_detail_cancel_confirm)
            .setPositiveButton(R.string.action_yes) { _, _ ->
                cancelOrderBuyer()
            }
            .setNegativeButton(R.string.action_no, null)
            .show()
    }

    private fun cancelOrderBuyer() {
        showLoading(true)

        OrderRepository.cancelOrder(
            orderId = orderId,
            onSuccess = {
                Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                loadOrderDetail()
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this, "Failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    /**
     * Buat row untuk satu item order.
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

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) binding.contentLayout.visibility = View.GONE
    }
}