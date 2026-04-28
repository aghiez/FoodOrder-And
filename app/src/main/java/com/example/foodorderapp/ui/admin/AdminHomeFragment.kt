package com.example.foodorderapp.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.data.repository.AdminRepository
import com.example.foodorderapp.databinding.FragmentAdminHomeBinding
import com.example.foodorderapp.ui.admin.adapter.AdminNewSellerAdapter
import com.example.foodorderapp.ui.admin.adapter.AdminRecentOrderAdapter
import com.example.foodorderapp.ui.buyer.OrderDetailActivity
import com.example.foodorderapp.utils.Formatter
import com.google.firebase.firestore.ListenerRegistration
import com.example.foodorderapp.ui.admin.AdminOrdersActivity

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    private val TAG = "AdminHomeFragment"

    private lateinit var recentOrderAdapter: AdminRecentOrderAdapter
    private lateinit var newSellerAdapter: AdminNewSellerAdapter

    private var ordersListener: ListenerRegistration? = null
    private var sellersListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        loadStats()
        startListeningToRecentOrders()
        startListeningToNewSellers()
    }

    private fun setupRecyclerViews() {
        // Recent Orders
        recentOrderAdapter = AdminRecentOrderAdapter(emptyList()) { order ->
            // Klik order → buka detail
            val intent = Intent(requireContext(), OrderDetailActivity::class.java)
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.orderId)
            // Untuk admin, kita pakai buyer mode (read-only)
            intent.putExtra(OrderDetailActivity.EXTRA_VIEW_MODE,
                OrderDetailActivity.MODE_BUYER)
            startActivity(intent)
        }
        binding.rvRecentOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentOrderAdapter
            isNestedScrollingEnabled = false
        }

        // New Sellers
        newSellerAdapter = AdminNewSellerAdapter(emptyList()) { seller ->
            // TODO: bisa ke detail seller (D.3 implementation)
            val name = seller["storeName"] as? String ?: seller["name"] as? String ?: "Seller"
            Toast.makeText(requireContext(),
                "Seller: $name (detail di D.3)", Toast.LENGTH_SHORT).show()
        }
        binding.rvNewSellers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newSellerAdapter
            isNestedScrollingEnabled = false
        }

        binding.tvViewAllOrders.setOnClickListener{
            startActivity(Intent(requireContext(), AdminOrdersActivity::class.java))
        }
    }

    private fun loadStats() {
        AdminRepository.getAdminStats(
            onSuccess = { stats ->
                if (_binding == null) return@getAdminStats

                binding.tvStatTotalUsers.text = stats.totalUsers.toString()
                binding.tvStatTotalSellers.text = stats.totalSellers.toString()
                binding.tvStatOrdersToday.text = stats.ordersToday.toString()
                binding.tvStatRevenueToday.text = Formatter.toRupiah(stats.revenueToday)
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    Log.e(TAG, "Failed to load stats: $errorMessage")
                    Toast.makeText(requireContext(),
                        "Failed to load stats", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun startListeningToRecentOrders() {
        ordersListener = AdminRepository.listenRecentOrders(
            limit = 5,
            onUpdate = { orders ->
                if (_binding == null) return@listenRecentOrders

                recentOrderAdapter.updateOrders(orders)
                binding.tvEmptyOrders.visibility = if (orders.isEmpty())
                    View.VISIBLE else View.GONE
                binding.rvRecentOrders.visibility = if (orders.isEmpty())
                    View.GONE else View.VISIBLE

                // Refresh stats juga karena ada update orders
                loadStats()
            },
            onError = { errorMessage ->
                Log.e(TAG, "Error loading recent orders: $errorMessage")
            }
        )
    }

    private fun startListeningToNewSellers() {
        sellersListener = AdminRepository.listenNewSellers(
            onUpdate = { sellers ->
                if (_binding == null) return@listenNewSellers

                newSellerAdapter.updateSellers(sellers)
                binding.tvEmptySellers.visibility = if (sellers.isEmpty())
                    View.VISIBLE else View.GONE
                binding.rvNewSellers.visibility = if (sellers.isEmpty())
                    View.GONE else View.VISIBLE

                // Refresh stats juga
                loadStats()
            },
            onError = { errorMessage ->
                Log.e(TAG, "Error loading new sellers: $errorMessage")
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ordersListener?.remove()
        ordersListener = null
        sellersListener?.remove()
        sellersListener = null
        _binding = null
    }
}