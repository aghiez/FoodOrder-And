package com.example.foodorderapp.data.repository

import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration

object MenuRepository {

    /**
     * Listen real-time semua menu milik seller saat ini.
     */
    fun listenToSellerMenus(
        onUpdate: (List<Food>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val sellerId = FirebaseHelper.getCurrentUserId()
        if (sellerId == null) {
            onError("User not logged in")
            return null
        }

        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val foods = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Food::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    // Sort manual: tersedia dulu, lalu by created date
                    val sorted = foods.sortedWith(
                        compareByDescending<Food> { it.isAvailable }
                            .thenByDescending { it.createdAt }
                    )
                    onUpdate(sorted)
                }
            }
    }

    /**
     * Toggle availability menu (true ↔ false).
     */
    fun toggleAvailability(
        foodId: String,
        newAvailability: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = mapOf(
            "isAvailable" to newAvailability,
            "updatedAt" to System.currentTimeMillis()
        )

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .document(foodId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update")
            }
    }

    /**
     * Delete menu permanently.
     * Note: Untuk production, lebih baik soft-delete (set isDeleted=true).
     */
    fun deleteMenu(
        foodId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .document(foodId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to delete")
            }
    }

    /**
     * Get single food by ID (untuk edit).
     */
    fun getMenuById(
        foodId: String,
        onSuccess: (Food) -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .document(foodId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val food = document.toObject(Food::class.java)?.copy(id = document.id)
                    if (food != null) {
                        onSuccess(food)
                    } else {
                        onFailure("Food data is corrupted")
                    }
                } else {
                    onFailure("Food not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Unknown error")
            }
    }
}