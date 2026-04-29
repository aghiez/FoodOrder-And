package com.example.foodorderapp.data.model

data class Order(
    val orderId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val deliveryAddress: String = "",
    val deliveryFee: Double = 0.0,
    val notes: String = "",
    val paymentMethod: String = "",
    val status: String = "pending",
    val isReviewed: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long = 0L
)

data class OrderItem(
    val foodId: String = "",
    val foodName: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val subtotal: Double = 0.0
)
