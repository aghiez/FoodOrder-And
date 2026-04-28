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
import com.example.foodorderapp.ui.DashboardActivity
import com.example.foodorderapp.ui.admin.AdminDashboardActivity
import com.example.foodorderapp.ui.buyer.BuyerDashboardActivity
import com.example.foodorderapp.ui.seller.SellerDashboardActivity

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
            // TODO: Implement forgot password (Tahap berikutnya)
            Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validasi
        if (!validateInput(email, password)) return

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
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                val errorMessage = when {
                    exception.message?.contains("password") == true ->
                        "Wrong password"
                    exception.message?.contains("no user record") == true ->
                        "Email not registered"
                    exception.message?.contains("badly formatted") == true ->
                        "Invalid email format"
                    else -> "Login failed: ${exception.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this, getString(R.string.msg_login_success),
                        Toast.LENGTH_SHORT).show()
                    navigateToDashboard(role)
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    FirebaseHelper.signOut()
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
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