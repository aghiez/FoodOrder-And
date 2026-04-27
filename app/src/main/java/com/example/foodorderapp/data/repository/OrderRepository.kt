package com.example.foodorderapp.data.repository

import com.example.foodorderapp.data.model.CartItem
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.model.OrderItem
import com.example.foodorderapp.data.model.OrderStatus
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object OrderRepository {

    private const val DELIVERY_FEE_DEFAULT = 5000.0

    /**
     * Buat order baru dari cart items.
     * Setelah order tersimpan, cart akan di-clear.
     */
    fun createOrder(
        cartItems: List<CartItem>,
        deliveryAddress: String,
        notes: String,
        paymentMethod: String,
        onSuccess: (orderId: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        if (cartItems.isEmpty()) {
            onFailure("Cart is empty")
            return
        }

        // Karena cart hanya boleh dari 1 seller, ambil sellerId dari item pertama
        val sellerId = cartItems.first().sellerId

        // Convert CartItem ke OrderItem (snapshot data)
        val orderItems = cartItems.map { cartItem ->
            OrderItem(
                foodId = cartItem.foodId,
                foodName = cartItem.foodName,
                quantity = cartItem.quantity,
                price = cartItem.price,
                subtotal = cartItem.subtotal
            )
        }

        val subtotal = cartItems.sumOf { it.subtotal }
        val totalAmount = subtotal + DELIVERY_FEE_DEFAULT
        val currentTime = System.currentTimeMillis()

        // Buat reference dulu untuk dapat orderId
        val orderRef = FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .document()

        val order = Order(
            orderId = orderRef.id,
            buyerId = userId,
            sellerId = sellerId,
            items = orderItems,
            totalAmount = totalAmount,
            deliveryAddress = deliveryAddress,
            deliveryFee = DELIVERY_FEE_DEFAULT,
            notes = notes,
            paymentMethod = paymentMethod,
            status = OrderStatus.PENDING,
            createdAt = currentTime,
            updatedAt = currentTime,
            completedAt = 0L
        )

        // Save order
        orderRef.set(order)
            .addOnSuccessListener {
                // Clear cart setelah order berhasil
                CartRepository.clearCart(
                    onSuccess = { onSuccess(orderRef.id) },
                    onFailure = {
                        // Order berhasil tapi cart gagal di-clear, tetap call onSuccess
                        onSuccess(orderRef.id)
                    }
                )
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to create order")
            }
    }

    /**
     * Listen real-time orders dari user yang sedang login (untuk Buyer).
     */
    fun listenToBuyerOrders(
        onUpdate: (List<Order>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onError("User not logged in")
            return null
        }

        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .whereEqualTo("buyerId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Order::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUpdate(orders)
                }
            }
    }

    /**
     * Get single order by ID.
     */
    fun getOrderById(
        orderId: String,
        onSuccess: (Order) -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val order = document.toObject(Order::class.java)
                    if (order != null) {
                        onSuccess(order)
                    } else {
                        onFailure("Order data is corrupted")
                    }
                } else {
                    onFailure("Order not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Cancel order (hanya bisa cancel jika status PENDING atau ACCEPTED).
     */
    fun cancelOrder(
        orderId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = mapOf(
            "status" to OrderStatus.CANCELLED,
            "updatedAt" to System.currentTimeMillis()
        )

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .document(orderId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to cancel order")
            }
    }

    /**
     * Helper untuk get readable status label.
     */
    fun getStatusLabel(status: String): String {
        return when (status) {
            OrderStatus.PENDING -> "Pending"
            OrderStatus.ACCEPTED -> "Accepted"
            OrderStatus.PREPARING -> "Preparing"
            OrderStatus.READY -> "Ready"
            OrderStatus.DELIVERED -> "Delivered"
            OrderStatus.CANCELLED -> "Cancelled"
            else -> status.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Helper untuk get drawable resource buat status badge.
     */
    fun getStatusDrawable(status: String): Int {
        return when (status) {
            OrderStatus.PENDING -> com.example.foodorderapp.R.drawable.bg_status_pending
            OrderStatus.ACCEPTED -> com.example.foodorderapp.R.drawable.bg_status_accepted
            OrderStatus.PREPARING -> com.example.foodorderapp.R.drawable.bg_status_preparing
            OrderStatus.READY -> com.example.foodorderapp.R.drawable.bg_status_accepted
            OrderStatus.DELIVERED -> com.example.foodorderapp.R.drawable.bg_status_delivered
            OrderStatus.CANCELLED -> com.example.foodorderapp.R.drawable.bg_status_cancelled
            else -> com.example.foodorderapp.R.drawable.bg_status_pending
        }
    }

    /**
     * Helper untuk get text color buat status.
     */
    fun getStatusTextColor(status: String): Int {
        return when (status) {
            OrderStatus.PENDING -> android.graphics.Color.parseColor("#856404")
            OrderStatus.ACCEPTED -> android.graphics.Color.parseColor("#0C5460")
            OrderStatus.PREPARING -> android.graphics.Color.parseColor("#7E4710")
            OrderStatus.READY -> android.graphics.Color.parseColor("#0C5460")
            OrderStatus.DELIVERED -> android.graphics.Color.parseColor("#155724")
            OrderStatus.CANCELLED -> android.graphics.Color.parseColor("#721C24")
            else -> android.graphics.Color.parseColor("#856404")
        }
    }

    /**
     * Cek apakah order bisa di-cancel oleh buyer.
     */
    fun canBeCancelled(status: String): Boolean {
        return status == OrderStatus.PENDING || status == OrderStatus.ACCEPTED
    }
}