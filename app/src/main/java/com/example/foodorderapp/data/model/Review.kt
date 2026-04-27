package com.example.foodorderapp.data.model

data class Review(
    val reviewId: String = "",
    val orderId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val foodId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val buyerName: String = "",
    val createdAt: Long = 0L
)
