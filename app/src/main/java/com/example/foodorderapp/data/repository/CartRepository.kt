package com.example.foodorderapp.data.repository

import com.example.foodorderapp.data.model.CartItem
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration

object CartRepository {

    /**
     * Add atau update item di cart.
     * Kalau item sudah ada, quantity-nya ditambah.
     */
    fun addToCart(
        food: Food,
        sellerName: String,
        quantity: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        val cartItemRef = FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CARTS)
            .document(userId)
            .collection("items")
            .document(food.id)

        // Cek dulu apakah cart kosong atau seller-nya sama
        getCartItems(
            onSuccess = { cartItems ->
                // Jika ada items dari seller berbeda → reject
                if (cartItems.isNotEmpty() &&
                    cartItems.first().sellerId != food.sellerId) {
                    onFailure("Cart can only contain items from one store. Clear cart first.")
                    return@getCartItems
                }

                // Cek item ini sudah ada di cart?
                val existingItem = cartItems.find { it.foodId == food.id }

                val newItem = CartItem(
                    foodId = food.id,
                    foodName = food.name,
                    foodDescription = food.description,
                    price = food.price,
                    quantity = (existingItem?.quantity ?: 0) + quantity,
                    imageUrl = food.imageUrl,
                    sellerId = food.sellerId,
                    sellerName = sellerName,
                    addedAt = System.currentTimeMillis()
                )

                cartItemRef.set(newItem)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Unknown error")
                    }
            },
            onFailure = { onFailure(it) }
        )
    }

    /**
     * Get all items di cart user saat ini.
     */
    fun getCartItems(
        onSuccess: (List<CartItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CARTS)
            .document(userId)
            .collection("items")
            .get()
            .addOnSuccessListener { documents ->
                val items = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CartItem::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                onSuccess(items)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Update quantity item di cart.
     */
    fun updateQuantity(
        foodId: String,
        newQuantity: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        if (newQuantity <= 0) {
            removeFromCart(foodId, onSuccess, onFailure)
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CARTS)
            .document(userId)
            .collection("items")
            .document(foodId)
            .update("quantity", newQuantity)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Hapus item dari cart.
     */
    fun removeFromCart(
        foodId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CARTS)
            .document(userId)
            .collection("items")
            .document(foodId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Hapus semua item di cart.
     */
    fun clearCart(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CARTS)
            .document(userId)
            .collection("items")
            .get()
            .addOnSuccessListener { documents ->
                val batch = FirebaseHelper.firestore.batch()
                for (doc in documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Unknown error")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Listen perubahan cart secara real-time.
     * Return ListenerRegistration agar bisa di-remove saat fragment destroyed.
     */
    fun listenToCartItems(
        onUpdate: (List<CartItem>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onError("User not logged in")
            return null
        }

        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CARTS)
            .document(userId)
            .collection("items")
            .orderBy("addedAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(CartItem::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUpdate(items)
                }
            }
    }

    /**
     * Hitung total semua item di cart.
     */
    fun calculateTotal(items: List<CartItem>): Double {
        return items.sumOf { it.subtotal }
    }
}