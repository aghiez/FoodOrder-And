package com.example.foodorderapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.UserRole
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.ui.admin.AdminDashboardActivity
import com.example.foodorderapp.ui.auth.LoginActivity
import com.example.foodorderapp.ui.buyer.BuyerDashboardActivity
import com.example.foodorderapp.ui.seller.SellerDashboardActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 2000L  // 2 detik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay sebelum check status login
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, SPLASH_DELAY)
    }

    private fun checkLoginStatus() {
        if (FirebaseHelper.isUserLoggedIn()) {
            // User sudah login, ambil role dari Firestore
            getUserRoleAndNavigate()
        } else {
            // Belum login, ke Login Screen
            navigateToLogin()
        }
    }

    private fun getUserRoleAndNavigate() {
        val userId = FirebaseHelper.getCurrentUserId() ?: return navigateToLogin()

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: UserRole.BUYER
                    navigateToDashboard(role)
                } else {
                    // Data user tidak ditemukan, force logout
                    FirebaseHelper.signOut()
                    navigateToLogin()
                }
            }
            .addOnFailureListener {
                navigateToLogin()
            }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToDashboard(role: String) {
        val intent = when(role){
            UserRole.BUYER -> Intent(this, BuyerDashboardActivity::class.java)
            UserRole.SELLER -> Intent(this, SellerDashboardActivity::class.java)
            UserRole.ADMIN -> Intent(this, AdminDashboardActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
 //       intent.putExtra("USER_ROLE", role)
        startActivity(intent)
        finish()
    }
}