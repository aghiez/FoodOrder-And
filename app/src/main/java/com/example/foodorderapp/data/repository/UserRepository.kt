package com.example.foodorderapp.data.repository

import com.example.foodorderapp.data.model.User
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.google.firebase.auth.EmailAuthProvider

object UserRepository {

    /**
     * Get current user data from Firestore.
     */
    fun getCurrentUser(
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure("User data is corrupted")
                    }
                } else {
                    onFailure("User not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Update user profile (name & phone).
     */
    fun updateProfile(
        name: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        val updates = mapOf(
            "name" to name,
            "phone" to phone,
            "updatedAt" to System.currentTimeMillis()
        )

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update profile")
            }
    }

    /**
     * Change password.
     * Firebase requires re-authentication before sensitive operations.
     */
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = FirebaseHelper.auth.currentUser
        if (user == null || user.email == null) {
            onFailure("User not logged in")
            return
        }

        // Step 1: Re-authenticate dengan password lama
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Step 2: Update password
                user.updatePassword(newPassword)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Failed to update password")
                    }
            }
            .addOnFailureListener {
                onFailure("Current password is incorrect")
            }
    }

    /**
     * Get statistics user (total orders, total spent).
     * Hanya hitung orders dengan status delivered (yang sudah selesai).
     */
    fun getUserStatistics(
        onSuccess: (totalOrders: Int, totalSpent: Double) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .whereEqualTo("buyerId", userId)
            .get()
            .addOnSuccessListener { documents ->
                // Hitung semua orders kecuali cancelled
                val validOrders = documents.documents.filter { doc ->
                    val status = doc.getString("status")
                    status != "cancelled"
                }

                val totalOrders = validOrders.size
                val totalSpent = validOrders.sumOf {
                    it.getDouble("totalAmount") ?: 0.0
                }

                onSuccess(totalOrders, totalSpent)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to load statistics")
            }
    }
}