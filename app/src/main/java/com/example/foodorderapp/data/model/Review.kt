//package com.example.foodorderapp.data.model
//
//data class Review(
//    val reviewId: String = "",
//    val orderId: String = "",
//    val buyerId: String = "",
//    val sellerId: String = "",
//    val foodId: String = "",
//    val rating: Int = 0,
//    val comment: String = "",
//    val buyerName: String = "",
//    val createdAt: Long = 0L
//)

package com.example.foodorderapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Model untuk review yang diberikan buyer ke menu setelah order delivered.
 *
 * Untuk Materi Ajar:
 * - 1 review terkait dengan 1 order (foreign key: orderId)
 * - 1 review untuk 1 menu (foreign key: foodId)
 * - 1 buyer bisa kasih banyak review (di order berbeda)
 */
@Parcelize
data class Review(
    val id: String = "",
    val orderId: String = "",       // FK ke orders collection
    val foodId: String = "",        // FK ke foods collection
    val sellerId: String = "",      // FK ke users (seller)
    val buyerId: String = "",       // FK ke users (buyer)
    val buyerName: String = "",     // Cached nama buyer
    val rating: Int = 0,            // 1-5 stars
    val comment: String = "",       // Review text
    val createdAt: Long = 0L
) : Parcelable