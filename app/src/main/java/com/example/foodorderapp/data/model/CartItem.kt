package com.example.foodorderapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val foodId: String = "",
    val foodName: String = "",
    val foodDescription: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val imageUrl: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val addedAt: Long = 0L
): Parcelable{
    val subtotal: Double
        get() = price * quantity
}
