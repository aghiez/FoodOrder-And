package com.example.foodorderapp.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.foodorderapp.R
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.AdminRepository
import com.example.foodorderapp.databinding.ActivityUserDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailBinding

    private var userId: String = ""
    private var userData: Map<String, Any>? = null

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPromote.setOnClickListener { handlePromote() }
        binding.btnDemote.setOnClickListener { handleDemote() }
        binding.btnToggleSuspend.setOnClickListener { handleToggleSuspend() }

        loadUserDetail()
    }

    private fun loadUserDetail() {
        showLoading(true)

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    val data = document.data?.toMutableMap() ?: mutableMapOf()
                    data["userId"] = document.id
                    userData = data
                    displayUser(data)
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayUser(user: Map<String, Any>) {
        val name = user["storeName"] as? String
            ?: user["name"] as? String ?: "Unknown"
        val email = user["email"] as? String ?: ""
        val phone = user["phone"] as? String ?: "-"
        val role = user["role"] as? String ?: "buyer"
        val isActive = user["isActive"] as? Boolean ?: true
        val createdAt = user["createdAt"] as? Long ?: 0L

        binding.tvUserName.text = name
        binding.tvUserEmailHeader.text = email
        binding.tvUserInitial.text = name.firstOrNull()?.uppercase() ?: "U"
        binding.tvUserPhone.text = phone

        // Role badge
        binding.tvUserRole.apply {
            when (role) {
                "buyer" -> {
                    text = getString(R.string.users_role_buyer)
                    setBackgroundResource(R.drawable.bg_role_buyer)
                    setTextColor(android.graphics.Color.parseColor("#7E4710"))
                }
                "seller" -> {
                    text = getString(R.string.users_role_seller)
                    setBackgroundResource(R.drawable.bg_role_seller)
                    setTextColor(android.graphics.Color.parseColor("#1565C0"))
                }
                "admin" -> {
                    text = getString(R.string.users_role_admin)
                    setBackgroundResource(R.drawable.bg_role_admin)
                    setTextColor(ContextCompat.getColor(this@UserDetailActivity, R.color.primary))
                }
            }
        }

        // Status
        if (isActive) {
            binding.tvUserStatus.text = getString(R.string.users_status_active)
            binding.tvUserStatus.setTextColor(
                ContextCompat.getColor(this, R.color.success))
        } else {
            binding.tvUserStatus.text = getString(R.string.users_status_suspended)
            binding.tvUserStatus.setTextColor(
                ContextCompat.getColor(this, R.color.error))
        }

        // Joined date
        if (createdAt > 0) {
            val format = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            binding.tvUserJoined.text = format.format(Date(createdAt))
        } else {
            binding.tvUserJoined.text = "-"
        }

        // Store info (untuk seller)
        if (role == "seller") {
            binding.llStoreInfoSection.visibility = View.VISIBLE
            binding.tvStoreName.text = user["storeName"] as? String ?: "-"
            binding.tvStoreAddress.text = user["storeAddress"] as? String ?: "-"
        } else {
            binding.llStoreInfoSection.visibility = View.GONE
        }

        // Setup action buttons
        setupActionButtons(role, isActive)
    }

    private fun setupActionButtons(role: String, isActive: Boolean) {
        // Cek: jangan modify diri sendiri
        val currentUserId = FirebaseHelper.getCurrentUserId()
        val isSelf = currentUserId == userId

        // Cek: jangan modify admin lain
        val isAdmin = role == "admin"

        // Promote button (hanya untuk buyer)
        binding.btnPromote.visibility = if (role == "buyer" && !isSelf) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Demote button (hanya untuk seller, bukan admin)
        binding.btnDemote.visibility = if (role == "seller" && !isSelf) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Suspend/Activate button (untuk semua kecuali admin & self)
        if (isSelf || isAdmin) {
            binding.btnToggleSuspend.visibility = View.GONE
        } else {
            binding.btnToggleSuspend.visibility = View.VISIBLE
            if (isActive) {
                binding.btnToggleSuspend.text = getString(R.string.users_action_suspend)
                binding.btnToggleSuspend.setTextColor(
                    ContextCompat.getColor(this, R.color.error))
            } else {
                binding.btnToggleSuspend.text = getString(R.string.users_action_activate)
                binding.btnToggleSuspend.setTextColor(
                    ContextCompat.getColor(this, R.color.success))
            }
        }
    }

    private fun handlePromote() {
        AlertDialog.Builder(this)
            .setTitle(R.string.users_confirm_promote_title)
            .setMessage(R.string.users_confirm_promote_message)
            .setPositiveButton(R.string.users_action_promote) { _, _ ->
                updateRole("seller", getString(R.string.users_promoted_success))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun handleDemote() {
        AlertDialog.Builder(this)
            .setTitle(R.string.users_confirm_demote_title)
            .setMessage(R.string.users_confirm_demote_message)
            .setPositiveButton(R.string.users_action_demote) { _, _ ->
                updateRole("buyer", getString(R.string.users_demoted_success))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun handleToggleSuspend() {
        val isActive = userData?.get("isActive") as? Boolean ?: true

        if (isActive) {
            // Suspend
            AlertDialog.Builder(this)
                .setTitle(R.string.users_confirm_suspend_title)
                .setMessage(R.string.users_confirm_suspend_message)
                .setPositiveButton(R.string.users_action_suspend) { _, _ ->
                    toggleSuspend(false, getString(R.string.users_suspended_success))
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } else {
            // Activate
            AlertDialog.Builder(this)
                .setTitle(R.string.users_confirm_activate_title)
                .setMessage(R.string.users_confirm_activate_message)
                .setPositiveButton(R.string.users_action_activate) { _, _ ->
                    toggleSuspend(true, getString(R.string.users_activated_success))
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    private fun updateRole(newRole: String, successMessage: String) {
        showLoading(true)

        AdminRepository.updateUserRole(
            userId = userId,
            newRole = newRole,
            onSuccess = {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                loadUserDetail()  // Reload
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this,
                    getString(R.string.users_action_failed) + ": $errorMessage",
                    Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun toggleSuspend(isActive: Boolean, successMessage: String) {
        showLoading(true)

        AdminRepository.toggleUserActiveStatus(
            userId = userId,
            isActive = isActive,
            onSuccess = {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                loadUserDetail()  // Reload
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this,
                    getString(R.string.users_action_failed) + ": $errorMessage",
                    Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}