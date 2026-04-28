package com.example.foodorderapp.data.model

object OrderStatus {
    const val PENDING = "pending"
    const val ACCEPTED = "accepted"
    const val PREPARING = "preparing"
    const val READY = "ready"
    const val SHIPPED = "shipped"
    const val DELIVERED = "delivered"
    const val CANCELLED = "cancelled"
}

object UserRole {
    const val ADMIN = "admin"
    const val BUYER = "buyer"
    const val SELLER = "seller"
}
