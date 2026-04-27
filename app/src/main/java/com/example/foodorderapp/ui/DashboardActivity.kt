package com.example.foodorderapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.databinding.ActivityDashboardBinding
import com.example.foodorderapp.ui.auth.LoginActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun loadUserData() {
        val userId = FirebaseHelper.getCurrentUserId() ?: return
        val role = intent.getStringExtra("USER_ROLE") ?: "unknown"

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val email = document.getString("email") ?: ""

                    binding.tvWelcome.text = "Welcome, $name!"
                    binding.tvRole.text = "Role: ${role.replaceFirstChar { it.uppercase() }}"
                    binding.tvEmail.text = email
                }
            }
    }

    private fun performLogout() {
        FirebaseHelper.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}