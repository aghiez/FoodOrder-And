package com.example.foodorderapp.data.model

data class Seller(
    val userId: String = "",
    val storeName: String = "",
    val storeDescription: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val storeImageUrl: String = "",
    val isVerified: Boolean = false,
    val isOpen: Boolean = true,
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val totalOrders: Int = 0,
    val createdAt: Long = 0L
)
