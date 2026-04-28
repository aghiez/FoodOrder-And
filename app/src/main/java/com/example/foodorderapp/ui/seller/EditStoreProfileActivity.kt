package com.example.foodorderapp.ui.seller

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorderapp.R
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.SellerRepository
import com.example.foodorderapp.databinding.ActivityEditStoreProfileBinding

class EditStoreProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditStoreProfileBinding

    // Original values untuk detect changes
    private var originalStoreName: String = ""
    private var originalDescription: String = ""
    private var originalAddress: String = ""
    private var originalPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditStoreProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        loadCurrentProfile()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            handleBackPress()
        }
        binding.btnSave.setOnClickListener { saveProfile() }
    }

    /**
     * Load data profil saat ini untuk pre-fill form.
     */
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
                    originalStoreName = document.getString("storeName")
                        ?: document.getString("name") ?: ""
                    originalDescription = document.getString("storeDescription") ?: ""
                    originalAddress = document.getString("storeAddress") ?: ""
                    originalPhone = document.getString("phone") ?: ""

                    binding.etStoreName.setText(originalStoreName)
                    binding.etDescription.setText(originalDescription)
                    binding.etAddress.setText(originalAddress)
                    binding.etPhone.setText(originalPhone)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to load: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Validate semua input.
     */
    private fun validateInputs(): Boolean {
        val storeName = binding.etStoreName.text.toString().trim()
        if (storeName.isEmpty()) {
            binding.etStoreName.error = getString(R.string.edit_store_error_name_required)
            binding.etStoreName.requestFocus()
            return false
        }
        if (storeName.length < 3) {
            binding.etStoreName.error = getString(R.string.edit_store_error_name_short)
            binding.etStoreName.requestFocus()
            return false
        }

        val description = binding.etDescription.text.toString().trim()
        if (description.isEmpty()) {
            binding.etDescription.error = getString(R.string.edit_store_error_description_required)
            binding.etDescription.requestFocus()
            return false
        }

        val address = binding.etAddress.text.toString().trim()
        if (address.isEmpty()) {
            binding.etAddress.error = getString(R.string.edit_store_error_address_required)
            binding.etAddress.requestFocus()
            return false
        }
        if (address.length < 10) {
            binding.etAddress.error = getString(R.string.edit_store_error_address_short)
            binding.etAddress.requestFocus()
            return false
        }

        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.etPhone.error = getString(R.string.edit_store_error_phone_required)
            binding.etPhone.requestFocus()
            return false
        }
        // Validasi format: hanya angka, awal 0 atau +62, panjang 10-15
        if (!phone.matches(Regex("^(\\+62|0)\\d{9,13}$"))) {
            binding.etPhone.error = getString(R.string.edit_store_error_phone_invalid)
            binding.etPhone.requestFocus()
            return false
        }

        return true
    }

    private fun saveProfile() {
        if (!validateInputs()) return

        val storeName = binding.etStoreName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        showLoading(true)
        binding.btnSave.text = getString(R.string.edit_store_saving)

        SellerRepository.updateStoreProfile(
            storeName = storeName,
            storeDescription = description,
            storeAddress = address,
            phone = phone,
            onSuccess = {
                Toast.makeText(this,
                    getString(R.string.edit_store_save_success),
                    Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { errorMessage ->
                showLoading(false)
                binding.btnSave.text = getString(R.string.edit_store_save)
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    /**
     * Cek apakah ada perubahan yang belum disimpan.
     */
    private fun hasUnsavedChanges(): Boolean {
        return binding.etStoreName.text.toString().trim() != originalStoreName ||
                binding.etDescription.text.toString().trim() != originalDescription ||
                binding.etAddress.text.toString().trim() != originalAddress ||
                binding.etPhone.text.toString().trim() != originalPhone
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges()) {
            showDiscardDialog()
        } else {
            finish()
        }
    }

    private fun showDiscardDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_store_discard_title)
            .setMessage(R.string.edit_store_discard_message)
            .setPositiveButton(R.string.edit_store_discard_confirm) { _, _ -> finish() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (hasUnsavedChanges()) {
            showDiscardDialog()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
        binding.btnBack.isEnabled = !show
    }
}