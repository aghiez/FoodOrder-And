package com.example.foodorderapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Seller
import com.example.foodorderapp.data.model.User
import com.example.foodorderapp.data.model.UserRole
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.databinding.ActivityRegisterBinding
import com.example.foodorderapp.ui.buyer.BuyerDashboardActivity
import com.example.foodorderapp.ui.seller.SellerDashboardActivity
import com.example.foodorderapp.utils.ErrorHandler
import com.example.foodorderapp.utils.NetworkUtil
import com.example.foodorderapp.utils.SnackbarHelper
import com.example.foodorderapp.utils.ValidationHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var selectedRole: String = UserRole.BUYER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupRealtimeValidation()
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
            finish()
        }
    }

    /**
     * Setup real-time validation untuk semua field.
     * Error muncul saat user ngetik, bukan tunggu submit.
     */
    private fun setupRealtimeValidation() {
        ValidationHelper.attachNameValidator(binding.etName, minLength = 3)
        ValidationHelper.attachEmailValidator(binding.etEmail)
        ValidationHelper.attachPhoneValidator(binding.etPhone)
        ValidationHelper.attachPasswordValidator(binding.etPassword)
        ValidationHelper.attachConfirmPasswordValidator(
            passwordField = binding.etPassword,
            confirmPasswordField = binding.etConfirmPassword
        )
        ValidationHelper.attachPhoneValidator((binding.etPhone))
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
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validasi input (submit-time, complement real-time validation)
        if (!validateInput(name, email, phone, password, confirmPassword)) return

        // Cek koneksi internet
        if (!NetworkUtil.isOnline(this)) {
            SnackbarHelper.showNoInternet(binding.root) {
                performRegister()  // Retry
            }
            return
        }

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
                    SnackbarHelper.showError(
                        view = binding.root,
                        message = getString(R.string.error_unknown)
                    )
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)

                val friendlyMessage = ErrorHandler.getFriendlyMessage(this, exception)
                SnackbarHelper.showErrorWithRetry(
                    view = binding.root,
                    message = friendlyMessage
                ) {
                    performRegister()
                }
            }
    }

    private fun saveUserToFirestore(
        userId: String,
        name: String,
        email: String,
        phone: String
    ) {
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
                if (selectedRole == UserRole.SELLER) {
                    createSellerDocument(userId, name, phone, currentTime)
                } else {
                    finishRegistration()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)

                // Rollback: hapus user di Auth karena gagal save ke Firestore
                FirebaseHelper.auth.currentUser?.delete()

                val friendlyMessage = ErrorHandler.getFriendlyMessage(this, exception)
                SnackbarHelper.showError(
                    view = binding.root,
                    message = friendlyMessage
                )
            }
    }

    private fun createSellerDocument(
        userId: String,
        name: String,
        phone: String,
        time: Long
    ) {
        val seller = Seller(
            userId = userId,
            storeName = "$name's Store",
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
            .addOnFailureListener { exception ->
                showLoading(false)

                val friendlyMessage = ErrorHandler.getFriendlyMessage(this, exception)
                SnackbarHelper.showError(
                    view = binding.root,
                    message = friendlyMessage
                )
            }
    }

    private fun finishRegistration() {
        showLoading(false)
        SnackbarHelper.showSuccess(
            view = binding.root,
            message = getString(R.string.msg_register_success)
        )
        // Delay sebentar agar Snackbar terlihat sebelum navigate
        binding.root.postDelayed({
            navigateToDashboard()
        }, 800)
    }

    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        // Pakai ValidationHelper untuk consistency
        val nameResult = ValidationHelper.validateName(name, minLength = 3)
        if (!nameResult.isValid) {
            binding.etName.error = nameResult.errorMessage
            binding.etName.requestFocus()
            return false
        }

        val emailResult = ValidationHelper.validateEmail(email)
        if (!emailResult.isValid) {
            binding.etEmail.error = emailResult.errorMessage
            binding.etEmail.requestFocus()
            return false
        }

        val phoneResult = ValidationHelper.validatePhone(phone)
        if (!phoneResult.isValid) {
            binding.etPhone.error = phoneResult.errorMessage
            binding.etPhone.requestFocus()
            return false
        }

        val passwordResult = ValidationHelper.validatePassword(password)
        if (!passwordResult.isValid) {
            binding.etPassword.error = passwordResult.errorMessage
            binding.etPassword.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Confirm password is required"
            binding.etConfirmPassword.requestFocus()
            return false
        }

        if (confirmPassword != password) {
            binding.etConfirmPassword.error = "Passwords don't match"
            binding.etConfirmPassword.requestFocus()
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
        val intent = when (selectedRole) {
            UserRole.BUYER -> Intent(this, BuyerDashboardActivity::class.java)
            UserRole.SELLER -> Intent(this, SellerDashboardActivity::class.java)
            else -> Intent(this, BuyerDashboardActivity::class.java)
        }
        intent.putExtra("USER_ROLE", selectedRole)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}