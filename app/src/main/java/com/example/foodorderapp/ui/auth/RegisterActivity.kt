package com.example.foodorderapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.User
import com.example.foodorderapp.data.model.UserRole
import com.example.foodorderapp.data.model.Seller
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.databinding.ActivityRegisterBinding
import com.example.foodorderapp.ui.DashboardActivity
import com.example.foodorderapp.ui.buyer.BuyerDashboardActivity
import com.example.foodorderapp.ui.seller.SellerDashboardActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var selectedRole: String = UserRole.BUYER  // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.tvRoleBuyer.setOnClickListener {
            selectRole(UserRole.BUYER)
        }

        binding.tvRoleSeller.setOnClickListener {
            selectRole(UserRole.SELLER)
        }

        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        binding.tvLogin.setOnClickListener {
            finish()  // kembali ke LoginActivity
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role

        when (role) {
            UserRole.BUYER -> {
                binding.tvRoleBuyer.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_role_selected)
                binding.tvRoleBuyer.setTextColor(
                    ContextCompat.getColor(this, R.color.primary))

                binding.tvRoleSeller.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_role_unselected)
                binding.tvRoleSeller.setTextColor(
                    ContextCompat.getColor(this, R.color.text_secondary))
            }
            UserRole.SELLER -> {
                binding.tvRoleSeller.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_role_selected)
                binding.tvRoleSeller.setTextColor(
                    ContextCompat.getColor(this, R.color.primary))

                binding.tvRoleBuyer.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_role_unselected)
                binding.tvRoleBuyer.setTextColor(
                    ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
    }

    private fun performRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validasi
        if (!validateInput(name, email, phone, password)) return

        showLoading(true)

        // Step 1: Create user di Firebase Auth
        FirebaseHelper.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                if (userId != null) {
                    // Step 2: Save user data ke Firestore
                    saveUserToFirestore(userId, name, email, phone)
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                val errorMessage = when {
                    exception.message?.contains("email address is already") == true ->
                        "Email already registered"
                    exception.message?.contains("weak") == true ->
                        "Password too weak"
                    else -> "Registration failed: ${exception.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserToFirestore(userId: String, name: String, email: String, phone: String) {
        val currentTime = System.currentTimeMillis()

        val user = User(
            uid = userId,
            email = email,
            name = name,
            phone = phone,
            role = selectedRole,
            photoUrl = "",
            createdAt = currentTime,
            updatedAt = currentTime,
            isActive = true
        )

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .set(user)
            .addOnSuccessListener {
                // Jika role seller, buat juga document di sellers collection
                if (selectedRole == UserRole.SELLER) {
                    createSellerDocument(userId, name, phone, currentTime)
                } else {
                    finishRegistration()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                // Hapus user di Auth jika gagal save ke Firestore (rollback)
                FirebaseHelper.auth.currentUser?.delete()
                Toast.makeText(this, "Failed to save data: ${exception.message}",
                    Toast.LENGTH_LONG).show()
            }
    }

    private fun createSellerDocument(userId: String, name: String, phone: String, time: Long) {
        val seller = Seller(
            userId = userId,
            storeName = "$name's Store",  // default
            storeDescription = "",
            storeAddress = "",
            storePhone = phone,
            storeImageUrl = "",
            isVerified = false,
            isOpen = true,
            rating = 0.0,
            totalReviews = 0,
            totalOrders = 0,
            createdAt = time
        )

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_SELLERS)
            .document(userId)
            .set(seller)
            .addOnSuccessListener {
                finishRegistration()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Failed to create seller profile",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun finishRegistration() {
        showLoading(false)
        Toast.makeText(this, getString(R.string.msg_register_success),
            Toast.LENGTH_SHORT).show()
        navigateToDashboard()
    }

    private fun validateInput(name: String, email: String, phone: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_name_empty)
            binding.etName.requestFocus()
            return false
        }

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

        if (phone.isEmpty()) {
            binding.etPhone.error = getString(R.string.error_phone_empty)
            binding.etPhone.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.error_password_empty)
            binding.etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = getString(R.string.error_password_short)
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        binding.btnRegister.text = if (show) "" else getString(R.string.register_button)
    }

    private fun navigateToDashboard() {
        val intent = when(selectedRole){
            UserRole.BUYER -> Intent(this, BuyerDashboardActivity::class.java)
            UserRole.SELLER -> Intent(this, SellerDashboardActivity::class.java)
            else -> Intent(this, DashboardActivity::class.java)
        }
        intent.putExtra("USER_ROLE", selectedRole)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}