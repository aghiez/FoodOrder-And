package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.foodorderapp.R
import com.example.foodorderapp.data.repository.CartRepository
import com.example.foodorderapp.databinding.ActivityBuyerDashboardBinding
import com.google.firebase.firestore.ListenerRegistration

class BuyerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyerDashboardBinding
    private var cartListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuyerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set HomeFragment sebagai default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        setupBottomNavigation()
        listenToCartBadge()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_cart -> CartFragment()
                R.id.nav_orders -> OrdersFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Listen real-time perubahan cart untuk update badge di tab Cart.
     */
    private fun listenToCartBadge() {
        cartListener = CartRepository.listenToCartItems(
            onUpdate = { items ->
                updateCartBadge(items.size)
            },
            onError = {
                updateCartBadge(0)
            }
        )
    }

    private fun updateCartBadge(itemCount: Int) {
        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_cart)
        if (itemCount > 0) {
            badge.isVisible = true
            badge.number = itemCount
        } else {
            badge.isVisible = false
        }
    }

    /**
     * Switch ke tab Orders programmatically.
     * Dipanggil setelah checkout sukses.
     */
    fun switchToOrdersTab() {
        binding.bottomNavigation.selectedItemId = R.id.nav_orders
    }

    override fun onDestroy() {
        super.onDestroy()
        cartListener?.remove()
        cartListener = null
    }
}