package com.example.foodorderapp.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Collection Names (constants)
    const val COLLECTION_USERS = "users"
    const val COLLECTION_SELLERS = "sellers"
    const val COLLECTION_FOODS = "foods"
    const val COLLECTION_ORDERS = "orders"
    const val COLLECTION_CATEGORIES = "categories"
    const val COLLECTION_REVIEWS = "reviews"
    const val COLLECTION_CARTS = "carts"

    // Helper functions
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun signOut() {
        auth.signOut()
    }
}