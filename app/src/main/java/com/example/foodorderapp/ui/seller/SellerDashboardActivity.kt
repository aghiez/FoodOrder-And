package com.example.foodorderapp.ui.seller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.foodorderapp.R
import com.example.foodorderapp.databinding.ActivitySellerDashboardBinding

class SellerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellerDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set SellerHome sebagai default
        if (savedInstanceState == null) {
            loadFragment(SellerHomeFragment())
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.sellerBottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.seller_nav_home -> SellerHomeFragment()
                R.id.seller_nav_menu -> MenuManagementFragment()
                R.id.seller_nav_orders -> OrderManagementFragment()
                R.id.seller_nav_profile -> StoreProfileFragment()
                else -> SellerHomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.sellerFragmentContainer, fragment)
            .commit()
    }

    /**
     * Switch ke tab Orders programmatically.
     */
    fun switchToOrdersTab() {
        binding.sellerBottomNavigation.selectedItemId = R.id.seller_nav_orders
    }
}