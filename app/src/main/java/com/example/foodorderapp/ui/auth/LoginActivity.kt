package com.example.foodorderapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.UserRole
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.databinding.ActivityLoginBinding
import com.example.foodorderapp.ui.admin.AdminDashboardActivity
import com.example.foodorderapp.ui.buyer.BuyerDashboardActivity
import com.example.foodorderapp.ui.seller.SellerDashboardActivity
import com.example.foodorderapp.utils.ErrorHandler
import com.example.foodorderapp.utils.NetworkUtil
import com.example.foodorderapp.utils.SnackbarHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            SnackbarHelper.showInfo(binding.root, "Feature coming soon")
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validasi input
        if (!validateInput(email, password)) return

        // Cek koneksi internet sebelum hit Firebase
        if (!NetworkUtil.isOnline(this)) {
            SnackbarHelper.showNoInternet(binding.root) {
                performLogin()  // Retry callback
            }
            return
        }

        // Tampilkan loading
        showLoading(true)

        // Login dengan Firebase Auth
        FirebaseHelper.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                if (userId != null) {
                    fetchUserRoleAndNavigate(userId)
                } else {
                    showLoading(false)
                    SnackbarHelper.showError(
                        view = binding.root,
                        message = getString(R.string.error_unknown)
                    )
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)

                // Convert technical error ke user-friendly message
                val friendlyMessage = ErrorHandler.getFriendlyMessage(this, exception)

                // Show Snackbar dengan retry button
                SnackbarHelper.showErrorWithRetry(
                    view = binding.root,
                    message = friendlyMessage
                ) {
                    performLogin()  // Retry
                }
            }
    }

    private fun fetchUserRoleAndNavigate(userId: String) {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    val role = document.getString("role") ?: UserRole.BUYER
                    val isActive = document.getBoolean("isActive") ?: true

                    // Cek apakah account suspended (dari E.7 / Tahap 5 D.3)
                    if (!isActive) {
                        FirebaseHelper.signOut()
                        SnackbarHelper.showError(
                            view = binding.root,
                            message = getString(R.string.error_auth_user_disabled)
                        )
                        return@addOnSuccessListener
                    }

                    SnackbarHelper.showSuccess(
                        view = binding.root,
                        message = getString(R.string.msg_login_success)
                    )
                    navigateToDashboard(role)
                } else {
                    SnackbarHelper.showError(
                        view = binding.root,
                        message = "User data not found"
                    )
                    FirebaseHelper.signOut()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)

                val friendlyMessage = ErrorHandler.getFriendlyMessage(this, exception)
                SnackbarHelper.showErrorWithRetry(
                    view = binding.root,
                    message = friendlyMessage
                ) {
                    fetchUserRoleAndNavigate(userId)  // Retry
                }
            }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = getString(R.string.error_email_empty)
            binding.etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = getString(R.string.error_email_invalid)
            binding.etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.error_password_empty)
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnLogin.text = if (show) "" else getString(R.string.login_button)
    }

    private fun navigateToDashboard(role: String) {
        val intent = when(role) {
            UserRole.BUYER -> Intent(this, BuyerDashboardActivity::class.java)
            UserRole.SELLER -> Intent(this, SellerDashboardActivity::class.java)
            UserRole.ADMIN -> Intent(this, AdminDashboardActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        intent.putExtra("USER_ROLE", role)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}