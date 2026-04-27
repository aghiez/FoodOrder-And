package com.example.foodorderapp.data.repository

import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue

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
     * Add new menu ke Firestore.
     * imageUrl bisa kosong dulu, di-update setelah upload selesai.
     */
    fun addMenu(
        food: Food,
        onSuccess: (foodId: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val sellerId = FirebaseHelper.getCurrentUserId()
        if (sellerId == null) {
            onFailure("User not logged in")
            return
        }

        // Buat reference dulu untuk dapat ID
        val foodRef = FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .document()

        val foodWithId = food.copy(
            id = foodRef.id,
            sellerId = sellerId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        foodRef.set(foodWithId)
            .addOnSuccessListener { onSuccess(foodRef.id) }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to add menu")
            }
    }

    /**
     * Update existing menu.
     */
    fun updateMenu(
        foodId: String,
        name: String,
        description: String,
        categoryId: String,
        price: Double,
        stock: Int,
        imageUrl: String? = null,  // null = tidak update gambar
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "description" to description,
            "categoryId" to categoryId,
            "price" to price,
            "stock" to stock,
            "updatedAt" to System.currentTimeMillis()
        )

        // Hanya update imageUrl jika ada (jadi kalau edit tanpa ganti foto, tidak overwrite)
        if (!imageUrl.isNullOrEmpty()) {
            updates["imageUrl"] = imageUrl
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .document(foodId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update menu")
            }
    }

    /**
     * Update hanya imageUrl di document (dipakai setelah upload selesai).
     */
    fun updateMenuImageUrl(
        foodId: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .document(foodId)
            .update("imageUrl", imageUrl, "updatedAt", System.currentTimeMillis())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update image")
            }
    }

    /**
     * Get all categories (untuk Spinner).
     */
    fun getCategories(
        onSuccess: (List<com.example.foodorderapp.data.model.Category>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CATEGORIES)
            .orderBy("order")
            .get()
            .addOnSuccessListener { documents ->
                val categories = documents.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.foodorderapp.data.model.Category::class.java)
                            ?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                onSuccess(categories)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to load categories")
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