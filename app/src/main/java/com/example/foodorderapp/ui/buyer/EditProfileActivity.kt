package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.User
import com.example.foodorderapp.data.repository.UserRepository
import com.example.foodorderapp.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var originalName: String = ""
    private var originalPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveChanges() }

        loadUserData()
    }

    private fun loadUserData() {
        showLoading(true)

        UserRepository.getCurrentUser(
            onSuccess = { user ->
                showLoading(false)
                displayUserData(user)
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun displayUserData(user: User) {
        binding.etEmail.setText(user.email)
        binding.etName.setText(user.name)
        binding.etPhone.setText(user.phone)

        originalName = user.name
        originalPhone = user.phone
    }

    private fun saveChanges() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        // Validasi
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_name_empty)
            binding.etName.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            binding.etPhone.error = getString(R.string.error_phone_empty)
            binding.etPhone.requestFocus()
            return
        }

        // Cek apakah ada perubahan
        if (name == originalName && phone == originalPhone) {
            Toast.makeText(this, getString(R.string.edit_profile_no_changes),
                Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        UserRepository.updateProfile(
            name = name,
            phone = phone,
            onSuccess = {
                showLoading(false)
                Toast.makeText(this, getString(R.string.edit_profile_success),
                    Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
    }
}