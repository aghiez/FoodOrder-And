package com.example.foodorderapp.data.repository

import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.model.OrderStatus
import com.example.foodorderapp.data.model.Seller
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Calendar

object SellerRepository {

    /**
     * Get seller info by current user.
     */
    fun getCurrentSeller(
        onSuccess: (Seller) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onFailure("User not logged in")
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_SELLERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val seller = document.toObject(Seller::class.java)
                    if (seller != null) {
                        onSuccess(seller)
                    } else {
                        onFailure("Seller data is corrupted")
                    }
                } else {
                    onFailure("Seller not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Get statistik seller untuk dashboard.
     * Multiple async queries digabung jadi 1 callback.
     */
    fun getDashboardStats(
        onSuccess: (revenueToday: Double, pendingOrders: Int,
                    activeMenus: Int, totalOrders: Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val sellerId = FirebaseHelper.getCurrentUserId()
        if (sellerId == null) {
            onFailure("User not logged in")
            return
        }

        // Variables untuk hasil tiap query
        var revenueToday = 0.0
        var pendingOrders = 0
        var activeMenus = 0
        var totalOrders = 0

        // Counter untuk track berapa query yang selesai
        var completedQueries = 0
        val totalQueries = 3

        fun checkAllDone() {
            completedQueries++
            if (completedQueries == totalQueries) {
                onSuccess(revenueToday, pendingOrders, activeMenus, totalOrders)
            }
        }

        // Query 1: All orders milik seller ini (untuk revenue & total orders)
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { documents ->
                val startOfDay = getStartOfDayMillis()
                val endOfDay = getEndOfDayMillis()

                // Filter di client-side untuk menghindari composite index
                val allOrders = documents.documents

                // Total orders (semua kecuali cancelled)
                totalOrders = allOrders.count {
                    it.getString("status") != OrderStatus.CANCELLED
                }

                // Revenue hari ini (yang sudah delivered, dalam range hari ini)
                revenueToday = allOrders
                    .filter { doc ->
                        val status = doc.getString("status")
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        status == OrderStatus.DELIVERED &&
                                createdAt in startOfDay..endOfDay
                    }
                    .sumOf { it.getDouble("totalAmount") ?: 0.0 }

                // Pending orders
                pendingOrders = allOrders.count {
                    it.getString("status") == OrderStatus.PENDING
                }

                checkAllDone()
            }
            .addOnFailureListener {
                // Tetap call checkAllDone agar UI tidak stuck
                checkAllDone()
            }

        // Query 2: Active menus
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { documents ->
                activeMenus = documents.size()
                checkAllDone()
            }
            .addOnFailureListener {
                checkAllDone()
            }

        // Query 3 (placeholder, kita pakai dummy)
        // Sebenarnya cukup 2 query, tapi untuk clarity kita anggap 3 phase
        checkAllDone()
    }

    /**
     * Listen real-time pending orders untuk seller.
     */
    fun listenToPendingOrders(
        onUpdate: (List<Order>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val sellerId = FirebaseHelper.getCurrentUserId()
        if (sellerId == null) {
            onError("User not logged in")
            return null
        }

        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("status", OrderStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Order::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    // Sort manual karena tidak bisa orderBy tanpa composite index
                    val sorted = orders.sortedByDescending { it.createdAt }
                    onUpdate(sorted)
                }
            }
    }

    /**
     * Helper: get start of today in milliseconds.
     */
    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Helper: get end of today in milliseconds.
     */
    private fun getEndOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }



    /**
     * Listen real-time data store profile untuk seller saat ini.
     */
    fun listenToStoreProfile(
        onUpdate: (Map<String, Any>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            onError("User not logged in")
            return null
        }

        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap()
                    onUpdate(data)
                }
            }
    }

    /**
     * Toggle store status: open / close.
     */
    fun toggleStoreStatus(
        isOpen: Boolean,
        onSuccess: () -> Unit,
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
            .update(
                mapOf(
                    "isOpen" to isOpen,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update")
            }
    }

    /**
     * Update store profile (edit profile).
     */
    fun updateStoreProfile(
        storeName: String,
        storeDescription: String,
        storeAddress: String,
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
            "storeName" to storeName,
            "storeDescription" to storeDescription,
            "storeAddress" to storeAddress,
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
     * Calculate total revenue & orders dari Firestore.
     * Query semua orders milik seller dengan status DELIVERED, jumlahkan total.
     */
    fun calculateStoreStats(
        onSuccess: (totalRevenue: Double, totalOrders: Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val sellerId = FirebaseHelper.getCurrentUserId()
        if (sellerId == null) {
            onFailure("User not logged in")
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("status", "delivered")
            .get()
            .addOnSuccessListener { documents ->
                var totalRevenue = 0.0
                var totalOrders = 0

                for (doc in documents) {
                    val amount = doc.getDouble("totalAmount") ?: 0.0
                    totalRevenue += amount
                    totalOrders++
                }

                onSuccess(totalRevenue, totalOrders)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to calculate stats")
            }
    }


    /**
     * Calculate average rating untuk semua menu seller.
     * Dipakai di StoreProfile untuk display rating toko.
     */
    fun calculateStoreRating(
        onSuccess: (avgRating: Double, totalReviews: Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val sellerId = FirebaseHelper.getCurrentUserId()
        if (sellerId == null) {
            onFailure("User not logged in")
            return
        }

        // Query semua reviews untuk seller ini
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_REVIEWS)
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { documents ->
                val totalReviews = documents.size()

                if (totalReviews == 0) {
                    onSuccess(0.0, 0)
                    return@addOnSuccessListener
                }

                val sumRating = documents.documents.sumOf {
                    (it.getLong("rating") ?: 0L).toInt()
                }
                val avgRating = sumRating.toDouble() / totalReviews

                onSuccess(avgRating, totalReviews)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to calculate store rating")
            }
    }

}