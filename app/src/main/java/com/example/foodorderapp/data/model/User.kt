package com.example.foodorderapp.data.model

import android.provider.ContactsContract.CommonDataKinds.Email
import java.net.URL

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = "", //"admin","buyer","seller"
    val photoUrl: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isActive: Boolean = true
){
    fun isAdmin(): Boolean = role == "admin"
    fun isBuyer(): Boolean = role == "buyer"
    fun isSeller(): Boolean = role == "seller"
}
