package com.example.foodorderapp.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorderapp.R
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.AdminRepository
import com.example.foodorderapp.databinding.ActivityEditAdminProfileBinding

class EditAdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAdminProfileBinding

    private var originalName: String = ""
    private var originalPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { handleBackPress() }
        binding.btnSave.setOnClickListener { saveProfile() }

        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        showLoading(true)

        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    originalName = document.getString("name") ?: ""
                    originalPhone = document.getString("phone") ?: ""
                    val email = document.getString("email") ?: ""

                    binding.etName.setText(originalName)
                    binding.etPhone.setText(originalPhone)
                    binding.etEmail.setText(email)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(): Boolean {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            binding.etName.requestFocus()
            return false
        }
        if (name.length < 3) {
            binding.etName.error = "Name too short"
            binding.etName.requestFocus()
            return false
        }

        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone is required"
            binding.etPhone.requestFocus()
            return false
        }
        if (!phone.matches(Regex("^(\\+62|0)\\d{9,13}$"))) {
            binding.etPhone.error = "Invalid phone format"
            binding.etPhone.requestFocus()
            return false
        }

        return true
    }

    private fun saveProfile() {
        if (!validateInputs()) return

        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        showLoading(true)
        binding.btnSave.text = "Saving…"

        AdminRepository.updateAdminProfile(
            name = name,
            phone = phone,
            onSuccess = {
                Toast.makeText(this,
                    getString(R.string.edit_admin_save_success),
                    Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { errorMessage ->
                showLoading(false)
                binding.btnSave.text = "Save Changes"
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun hasUnsavedChanges(): Boolean {
        return binding.etName.text.toString().trim() != originalName ||
                binding.etPhone.text.toString().trim() != originalPhone
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("Your changes will be lost.")
                .setPositiveButton("Discard") { _, _ -> finish() }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } else {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPress()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
        binding.btnBack.isEnabled = !show
    }
}