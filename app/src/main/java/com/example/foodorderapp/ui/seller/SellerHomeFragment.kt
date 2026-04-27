package com.example.foodorderapp.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.repository.SellerRepository
import com.example.foodorderapp.databinding.FragmentSellerHomeBinding
import com.example.foodorderapp.ui.seller.adapter.PendingOrderAdapter
import com.example.foodorderapp.utils.Formatter
import com.google.firebase.firestore.ListenerRegistration

class SellerHomeFragment : Fragment() {

    private var _binding: FragmentSellerHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var pendingOrderAdapter: PendingOrderAdapter
    private var pendingOrdersListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        loadStoreInfo()
        loadStatistics()
        startListeningPendingOrders()
    }

    override fun onResume() {
        super.onResume()
        // Refresh statistics setiap kali fragment terlihat
        loadStatistics()
    }

    private fun setupRecyclerView() {
        pendingOrderAdapter = PendingOrderAdapter(emptyList()) { order ->
            // TODO: Navigate to OrderDetail di Seller side (akan dibuat di C.5)
            Toast.makeText(requireContext(),
                "Order detail: ${order.orderId.takeLast(8)}", Toast.LENGTH_SHORT).show()
        }

        binding.rvPendingOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendingOrderAdapter
        }
    }

    private fun setupListeners() {
        binding.btnViewAllOrders.setOnClickListener {
            // Switch ke tab Orders
            (activity as? SellerDashboardActivity)?.switchToOrdersTab()
        }
    }

    private fun loadStoreInfo() {
        SellerRepository.getCurrentSeller(
            onSuccess = { seller ->
                if (_binding != null) {
                    binding.tvStoreName.text = seller.storeName
                }
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    binding.tvStoreName.text = "My Store"
                }
            }
        )
    }

    private fun loadStatistics() {
        SellerRepository.getDashboardStats(
            onSuccess = { revenueToday, pendingOrders, activeMenus, totalOrders ->
                if (_binding == null) return@getDashboardStats

                binding.tvStatRevenue.text = Formatter.toRupiahShort(revenueToday)
                binding.tvStatPending.text = pendingOrders.toString()
                binding.tvStatMenu.text = activeMenus.toString()
                binding.tvStatTotalOrders.text = totalOrders.toString()
            },
            onFailure = { errorMessage ->
                if (_binding == null) return@getDashboardStats

                // Set defaults pada error
                binding.tvStatRevenue.text = "Rp 0"
                binding.tvStatPending.text = "0"
                binding.tvStatMenu.text = "0"
                binding.tvStatTotalOrders.text = "0"
            }
        )
    }

    private fun startListeningPendingOrders() {
        showLoading(true)

        pendingOrdersListener = SellerRepository.listenToPendingOrders(
            onUpdate = { orders ->
                if (_binding == null) return@listenToPendingOrders

                showLoading(false)
                updatePendingOrdersUI(orders)
            },
            onError = { errorMessage ->
                if (_binding == null) return@listenToPendingOrders

                showLoading(false)
                Toast.makeText(requireContext(),
                    "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updatePendingOrdersUI(orders: List<Order>) {
        // Tampilkan max 3 pending orders di home
        val displayOrders = orders.take(3)
        pendingOrderAdapter.updateOrders(displayOrders)

        val isEmpty = orders.isEmpty()
        binding.emptyPendingLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvPendingOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.btnViewAllOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressPendingOrders.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pendingOrdersListener?.remove()
        pendingOrdersListener = null
        _binding = null
    }
}