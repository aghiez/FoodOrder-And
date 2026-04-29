package com.example.foodorderapp.data.repository

import com.example.foodorderapp.data.model.Review
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object ReviewRepository {

    /**
     * Submit review baru.
     * Setelah berhasil, juga update rating aggregate di menu.
     */
    fun submitReview(
        orderId: String,
        foodId: String,
        sellerId: String,
        buyerName: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val buyerId = FirebaseHelper.getCurrentUserId()
        if (buyerId == null) {
            onFailure("User not logged in")
            return
        }

        // Step 1: Create review document
        val reviewRef = FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_REVIEWS)
            .document()

        val review = Review(
            id = reviewRef.id,
            orderId = orderId,
            foodId = foodId,
            sellerId = sellerId,
            buyerId = buyerId,
            buyerName = buyerName,
            rating = rating,
            comment = comment.trim(),
            createdAt = System.currentTimeMillis()
        )

        reviewRef.set(review)
            .addOnSuccessListener {
                // Step 2: Update aggregate rating di menu
                updateMenuRating(foodId,
                    onSuccess = {
                        // Step 3: Mark order sebagai sudah di-review
                        markOrderReviewed(orderId,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    },
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to submit review")
            }
    }

    /**
     * Update aggregate rating di menu (avg rating, total reviews).
     * Dipanggil otomatis setelah review baru.
     */
    private fun updateMenuRating(
        foodId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get semua reviews untuk menu ini
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_REVIEWS)
            .whereEqualTo("foodId", foodId)
            .get()
            .addOnSuccessListener { documents ->
                val totalReviews = documents.size()
                val sumRating = documents.documents.sumOf {
                    (it.getLong("rating") ?: 0L).toInt()
                }
                val avgRating = if (totalReviews > 0) {
                    (sumRating.toDouble() / totalReviews)
                } else {
                    0.0
                }

                // Update field rating di document menu
                FirebaseHelper.firestore
                    .collection(FirebaseHelper.COLLECTION_FOODS)
                    .document(foodId)
                    .update(
                        mapOf(
                            "rating" to avgRating,
                            "totalReviews" to totalReviews
                        )
                    )
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Failed to update menu rating")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to calculate rating")
            }
    }

    /**
     * Mark order sebagai sudah di-review (cegah review duplicate).
     */
    private fun markOrderReviewed(
        orderId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .document(orderId)
            .update("isReviewed", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                // Tidak fatal kalau gagal, review tetap tersimpan
                onSuccess()  // tetap dianggap sukses
            }
    }

    /**
     * Listen real-time reviews untuk satu menu.
     */
    fun listenReviewsForFood(
        foodId: String,
        onUpdate: (List<Review>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_REVIEWS)
            .whereEqualTo("foodId", foodId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val reviews = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Review::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    // Sort newest first di client
                    val sorted = reviews.sortedByDescending { it.createdAt }
                    onUpdate(sorted)
                }
            }
    }

    /**
     * Cek apakah order sudah di-review (untuk hide tombol Write Review).
     */
    fun checkOrderReviewed(
        orderId: String,
        onResult: (isReviewed: Boolean) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                val isReviewed = doc.getBoolean("isReviewed") ?: false
                onResult(isReviewed)
            }
            .addOnFailureListener {
                onResult(false)  // default: belum di-review
            }
    }
}