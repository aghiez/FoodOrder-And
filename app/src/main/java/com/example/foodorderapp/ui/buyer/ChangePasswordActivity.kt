package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorderapp.R
import com.example.foodorderapp.data.repository.UserRepository
import com.example.foodorderapp.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnUpdatePassword.setOnClickListener { updatePassword() }
    }

    private fun updatePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validasi
        if (currentPassword.isEmpty()) {
            binding.etCurrentPassword.error = getString(R.string.error_password_empty)
            binding.etCurrentPassword.requestFocus()
            return
        }

        if (newPassword.isEmpty()) {
            binding.etNewPassword.error = getString(R.string.error_password_empty)
            binding.etNewPassword.requestFocus()
            return
        }

        if (newPassword.length < 6) {
            binding.etNewPassword.error = getString(R.string.error_password_short)
            binding.etNewPassword.requestFocus()
            return
        }

        if (newPassword != confirmPassword) {
            binding.etConfirmPassword.error = getString(R.string.error_password_mismatch)
            binding.etConfirmPassword.requestFocus()
            return
        }

        if (currentPassword == newPassword) {
            binding.etNewPassword.error = getString(R.string.error_password_same)
            binding.etNewPassword.requestFocus()
            return
        }

        showLoading(true)

        UserRepository.changePassword(
            currentPassword = currentPassword,
            newPassword = newPassword,
            onSuccess = {
                showLoading(false)
                Toast.makeText(this, getString(R.string.change_password_success),
                    Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { errorMessage ->
                showLoading(false)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnUpdatePassword.isEnabled = !show
    }
}