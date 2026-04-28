package com.example.foodorderapp.data.repository

import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import com.example.foodorderapp.data.model.Category


object AdminRepository {

    /**
     * Statistik global untuk Admin Dashboard.
     */
    data class AdminStats(
        val totalUsers: Int = 0,
        val totalSellers: Int = 0,
        val totalBuyers: Int = 0,
        val ordersToday: Int = 0,
        val revenueToday: Double = 0.0
    )

    /**
     * Kalkulasi semua statistik admin.
     * Multi-step query, callback dipanggil setelah semua selesai.
     */
    fun getAdminStats(
        onSuccess: (AdminStats) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Step 1: Count users by role
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .get()
            .addOnSuccessListener { userDocs ->
                var totalUsers = 0
                var totalSellers = 0
                var totalBuyers = 0

                for (doc in userDocs) {
                    totalUsers++
                    when (doc.getString("role")) {
                        "seller" -> totalSellers++
                        "buyer" -> totalBuyers++
                    }
                }

                // Step 2: Get orders today
                val todayStart = getTodayStartTimestamp()

                FirebaseHelper.firestore
                    .collection(FirebaseHelper.COLLECTION_ORDERS)
                    .whereGreaterThanOrEqualTo("createdAt", todayStart)
                    .get()
                    .addOnSuccessListener { orderDocs ->
                        var ordersToday = 0
                        var revenueToday = 0.0

                        for (doc in orderDocs) {
                            ordersToday++
                            // Hanya count revenue dari order yang delivered
                            val status = doc.getString("status") ?: ""
                            if (status == "delivered") {
                                revenueToday += doc.getDouble("totalAmount") ?: 0.0
                            }
                        }

                        onSuccess(AdminStats(
                            totalUsers = totalUsers,
                            totalSellers = totalSellers,
                            totalBuyers = totalBuyers,
                            ordersToday = ordersToday,
                            revenueToday = revenueToday
                        ))
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Failed to fetch orders")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to fetch users")
            }
    }

    /**
     * Listen recent orders (max 5) untuk Admin Home.
     */
    fun listenRecentOrders(
        limit: Long = 5,
        onUpdate: (List<Order>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
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
                    onUpdate(orders)
                }
            }
    }

    /**
     * Listen new sellers dalam 7 hari terakhir.
     */
    fun listenNewSellers(
        onUpdate: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .whereEqualTo("role", "seller")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Filter di client-side untuk hindari composite index
                    val newSellers = snapshot.documents
                        .mapNotNull { doc ->
                            val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                            data["userId"] = doc.id
                            data
                        }
                        .filter {
                            (it["createdAt"] as? Long ?: 0L) >= sevenDaysAgo
                        }
                        .sortedByDescending {
                            it["createdAt"] as? Long ?: 0L
                        }
                        .take(5)

                    onUpdate(newSellers)
                }
            }
    }

    /**
     * Helper: dapatkan timestamp awal hari ini (00:00:00).
     */
    private fun getTodayStartTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Listen real-time semua users.
     */
    fun listenAllUsers(
        onUpdate: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                        data["userId"] = doc.id
                        data
                    }
                    onUpdate(users)
                }
            }
    }

    /**
     * Filter users berdasarkan tab.
     */
    enum class UserTab {
        ALL, BUYERS, SELLERS, ADMINS
    }

    fun filterUsersByTab(users: List<Map<String, Any>>, tab: UserTab): List<Map<String, Any>> {
        return when (tab) {
            UserTab.ALL -> users
            UserTab.BUYERS -> users.filter { it["role"] == "buyer" }
            UserTab.SELLERS -> users.filter { it["role"] == "seller" }
            UserTab.ADMINS -> users.filter { it["role"] == "admin" }
        }
    }

    /**
     * Search users by name atau email.
     */
    fun searchUsers(users: List<Map<String, Any>>, query: String): List<Map<String, Any>> {
        if (query.isBlank()) return users

        val lowerQuery = query.lowercase().trim()
        return users.filter { user ->
            val name = (user["name"] as? String ?: "").lowercase()
            val email = (user["email"] as? String ?: "").lowercase()
            val storeName = (user["storeName"] as? String ?: "").lowercase()

            name.contains(lowerQuery) ||
                    email.contains(lowerQuery) ||
                    storeName.contains(lowerQuery)
        }
    }

    /**
     * Update role user (promote/demote).
     */
    fun updateUserRole(
        userId: String,
        newRole: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = mapOf(
            "role" to newRole,
            "updatedAt" to System.currentTimeMillis()
        )

        // Kalau di-promote ke seller, tambah default field seller
        val updatesWithDefaults = if (newRole == "seller") {
            updates + mapOf(
                "isOpen" to true,
                "totalRevenue" to 0.0,
                "totalOrders" to 0
            )
        } else {
            updates
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .update(updatesWithDefaults)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update role")
            }
    }

    /**
     * Toggle user active status (suspend/activate).
     */
    fun toggleUserActiveStatus(
        userId: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .update(
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update status")
            }
    }

    /**
     * Listen real-time semua categories.
     */
    fun listenAllCategories(
        onUpdate: (List<Category>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CATEGORIES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val categories = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Category::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val sorted = categories.sortedBy { it.order }
                    onUpdate(sorted)
                }
            }
    }

    /**
     * Add kategori baru.
     */
    fun addCategory(
        name: String,
        order: Int,
        onSuccess: (categoryId: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Generate document reference dengan ID otomatis
        val categoryRef = FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CATEGORIES)
            .document()

        val category = mapOf(
            "id" to categoryRef.id,
            "name" to name,
            "imageUrl" to "",
            "order" to order
        )

        categoryRef.set(category)
            .addOnSuccessListener { onSuccess(categoryRef.id) }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to add category")
            }
    }

    /**
     * Update kategori.
     */
    fun updateCategory(
        categoryId: String,
        name: String,
        order: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = mapOf(
            "name" to name,
            "order" to order
        )

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CATEGORIES)
            .document(categoryId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update category")
            }
    }

    /**
     * Delete kategori.
     */
    fun deleteCategory(
        categoryId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CATEGORIES)
            .document(categoryId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to delete category")
            }
    }

    /**
     * Cek berapa menu yang pakai kategori tertentu.
     */
    fun countMenusInCategory(
        categoryId: String,
        onSuccess: (count: Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .whereEqualTo("categoryId", categoryId)
            .get()
            .addOnSuccessListener { documents ->
                onSuccess(documents.size())
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to count menus")
            }
    }

    /**
     * Cek apakah ada kategori dengan nama yang sama (untuk validasi duplicate).
     */
    fun checkCategoryNameExists(
        name: String,
        excludeId: String? = null,
        onResult: (exists: Boolean) -> Unit
    ) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CATEGORIES)
            .get()
            .addOnSuccessListener { documents ->
                val exists = documents.any { doc ->
                    val docName = (doc.getString("name") ?: "").lowercase()
                    doc.id != excludeId && docName == name.lowercase()
                }
                onResult(exists)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    /**
     * Listen real-time semua orders untuk admin monitoring.
     */
    fun listenAllOrders(
        onUpdate: (List<Order>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_ORDERS)
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
                    // Sort newest first
                    val sorted = orders.sortedByDescending { it.createdAt }
                    onUpdate(sorted)
                }
            }
    }

    /**
     * Search orders by ID atau buyer name.
     */
    fun searchOrders(
        orders: List<Order>,
        query: String,
        buyerNameMap: Map<String, String> = emptyMap()
    ): List<Order> {
        if (query.isBlank()) return orders

        val lowerQuery = query.lowercase().trim()
        return orders.filter { order ->
            val orderId = order.orderId.lowercase()
            val buyerName = (buyerNameMap[order.buyerId] ?: "").lowercase()

            orderId.contains(lowerQuery) || buyerName.contains(lowerQuery)
        }
    }

    /**
     * Listen real-time data admin profile.
     */
    fun listenAdminProfile(
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
     * Update admin profile (hanya name dan phone).
     */
    fun updateAdminProfile(
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
     * Get summary stats untuk admin profile (read-only display).
     */
    fun getProfileStats(
        onSuccess: (totalUsers: Int, totalOrders: Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Count users
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .get()
            .addOnSuccessListener { userDocs ->
                val totalUsers = userDocs.size()

                // Count orders
                FirebaseHelper.firestore
                    .collection(FirebaseHelper.COLLECTION_ORDERS)
                    .get()
                    .addOnSuccessListener { orderDocs ->
                        onSuccess(totalUsers, orderDocs.size())
                    }
                    .addOnFailureListener {
                        onSuccess(totalUsers, 0)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to load stats")
            }
    }

}