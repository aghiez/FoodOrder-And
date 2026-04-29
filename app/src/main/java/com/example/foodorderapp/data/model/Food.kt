package com.example.foodorderapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Food(
    val id: String = "",
    val sellerId: String = "",
    val categoryId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val isAvailable: Boolean = true,
    val stock: Int = 0,
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val totalSold: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Parcelable
