package com.example.foodorderapp.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.foodorderapp.R
import com.example.foodorderapp.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        // Default fragment: Home
        if (savedInstanceState == null) {
            replaceFragment(AdminHomeFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_admin_home -> AdminHomeFragment()
                R.id.nav_admin_users -> UserManagementFragment()
                R.id.nav_admin_categories -> CategoryManagementFragment()
                R.id.nav_admin_profile -> AdminProfileFragment()
                else -> AdminHomeFragment()
            }
            replaceFragment(fragment)
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Navigate ke tab tertentu (untuk dipanggil dari fragment).
     */
    fun navigateToTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }
}