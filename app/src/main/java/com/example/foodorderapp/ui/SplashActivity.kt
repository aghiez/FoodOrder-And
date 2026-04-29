package com.example.foodorderapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.UserRole
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.databinding.ActivitySplashBinding
import com.example.foodorderapp.ui.admin.AdminDashboardActivity
import com.example.foodorderapp.ui.auth.LoginActivity
import com.example.foodorderapp.ui.buyer.BuyerDashboardActivity
import com.example.foodorderapp.ui.seller.SellerDashboardActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    /**
     * Minimum durasi splash agar branding terlihat.
     * Bisa di-tune untuk balance UX.
     */
    private val SPLASH_DURATION_MS = 1500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup status bar transparent untuk splash
        setupStatusBar()

        // Play animations
        playEntryAnimations()

        // Schedule navigation setelah splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DURATION_MS)
    }

    /**
     * Setup status bar agar match dengan splash gradient.
     */
    private fun setupStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
    }

    /**
     * Play entry animations untuk elements splash.
     */
    private fun playEntryAnimations() {
        // Logo: scale + fade in
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        binding.ivLogo.startAnimation(logoAnim)

        // App name: slide up + fade in (delayed)
        val nameAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        nameAnim.startOffset = 300
        binding.tvAppName.startAnimation(nameAnim)

        // Tagline: fade in (more delayed)
        val taglineAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        taglineAnim.startOffset = 600
        binding.tvTagline.startAnimation(taglineAnim)

        // Progress bar: fade in (latest)
        val progressAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        progressAnim.startOffset = 900
        binding.progressBar.startAnimation(progressAnim)
    }

    /**
     * Cek auth state lalu route ke dashboard yang sesuai.
     */
    private fun navigateToNextScreen() {
        val currentUser = FirebaseHelper.auth.currentUser

        if (currentUser == null) {
            // Belum login → ke LoginActivity
            navigateTo(LoginActivity::class.java)
            return
        }

        // Sudah login → cek role dari Firestore
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: UserRole.BUYER
                    navigateBasedOnRole(role)
                } else {
                    // Document tidak ada (corner case)
                    navigateTo(LoginActivity::class.java)
                }
            }
            .addOnFailureListener {
                // Network error atau lain-lain → fallback ke login
                navigateTo(LoginActivity::class.java)
            }
    }

    /**
     * Route ke dashboard sesuai role.
     */
    private fun navigateBasedOnRole(role: String) {
        val targetActivity = when (role) {
            UserRole.ADMIN -> AdminDashboardActivity::class.java
            UserRole.SELLER -> SellerDashboardActivity::class.java
            UserRole.BUYER -> BuyerDashboardActivity::class.java
            else -> LoginActivity::class.java
        }
        navigateTo(targetActivity)
    }

    /**
     * Navigate ke target dengan smooth transition.
     */
    private fun navigateTo(targetActivity: Class<*>) {
        val intent = Intent(this, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Smooth fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        finish()
    }
}