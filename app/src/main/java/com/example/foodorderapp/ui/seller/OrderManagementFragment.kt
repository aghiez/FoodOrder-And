package com.example.foodorderapp.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.model.OrderStatus
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.FragmentOrderManagementBinding
import com.example.foodorderapp.ui.buyer.OrderDetailActivity
import com.example.foodorderapp.ui.seller.adapter.SellerOrderAdapter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.ListenerRegistration
import android.content.Intent

class OrderManagementFragment : Fragment() {

    private var _binding: FragmentOrderManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var orderAdapter: SellerOrderAdapter
    private var allOrders: List<Order> = emptyList()
    private var currentTab: OrderRepository.OrderTab = OrderRepository.OrderTab.PENDING
    private var ordersListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        startListeningToOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = SellerOrderAdapter(
            orders = emptyList(),
            onOrderClick = { order ->
                // TODO: Akan ke OrderDetail di C.5.3
            val intent = Intent(requireContext(), OrderDetailActivity::class.java)
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.orderId)
                intent.putExtra(OrderDetailActivity.EXTRA_VIEW_MODE,OrderDetailActivity.MODE_SELLER)
                startActivity(intent)
            },
            onPrimaryAction = { order -> handlePrimaryAction(order) },
            onReject = { order -> handleReject(order) }
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = when (tab?.position) {
                    0 -> OrderRepository.OrderTab.PENDING
                    1 -> OrderRepository.OrderTab.ACTIVE
                    2 -> OrderRepository.OrderTab.COMPLETED
                    else -> OrderRepository.OrderTab.PENDING
                }
                applyFilter()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun startListeningToOrders() {
        showLoading(true)

        ordersListener = OrderRepository.listenToSellerOrders(
            onUpdate = { orders ->
                if (_binding == null) return@listenToSellerOrders

                showLoading(false)
                allOrders = orders
                applyFilter()
            },
            onError = { errorMessage ->
                if (_binding == null) return@listenToSellerOrders

                showLoading(false)
                Toast.makeText(requireContext(),
                    "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * Apply filter berdasarkan tab yang dipilih.
     */
    private fun applyFilter() {
        if (_binding == null) return

        val filtered = OrderRepository.filterOrdersByTab(allOrders, currentTab)
        orderAdapter.updateOrders(filtered)

        // Update count & empty state
        binding.tvOrderCount.text = "${allOrders.size} total orders"
        updateEmptyState(filtered.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE

        // Set empty state text berdasarkan tab
        when (currentTab) {
            OrderRepository.OrderTab.PENDING -> {
                binding.tvEmptyTitle.text = getString(R.string.seller_orders_empty_pending)
                binding.tvEmptySubtitle.text = getString(R.string.seller_orders_empty_pending_subtitle)
            }
            OrderRepository.OrderTab.ACTIVE -> {
                binding.tvEmptyTitle.text = getString(R.string.seller_orders_empty_active)
                binding.tvEmptySubtitle.text = getString(R.string.seller_orders_empty_active_subtitle)
            }
            OrderRepository.OrderTab.COMPLETED -> {
                binding.tvEmptyTitle.text = getString(R.string.seller_orders_empty_completed)
                binding.tvEmptySubtitle.text = getString(R.string.seller_orders_empty_completed_subtitle)
            }
        }
    }

    /**
     * Handle primary action button (Accept, Start Preparing, Mark Ready, Mark Delivered).
     */
    private fun handlePrimaryAction(order: Order) {
        when (order.status) {
            OrderStatus.PENDING -> showAcceptConfirmation(order)
            OrderStatus.ACCEPTED -> updateStatus(order, OrderStatus.PREPARING,
                getString(R.string.seller_order_preparing))
            OrderStatus.PREPARING -> updateStatus(order, OrderStatus.READY,
                getString(R.string.seller_order_ready))
            OrderStatus.READY -> updateStatus(order, OrderStatus.DELIVERED,
                getString(R.string.seller_order_delivered))
        }
    }

    private fun handleReject(order: Order) {
        showRejectConfirmation(order)
    }

    private fun showAcceptConfirmation(order: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.seller_confirm_accept_title)
            .setMessage(R.string.seller_confirm_accept_message)
            .setPositiveButton(R.string.seller_action_accept) { _, _ ->
                updateStatus(order, OrderStatus.ACCEPTED,
                    getString(R.string.seller_order_accepted))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showRejectConfirmation(order: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.seller_confirm_reject_title)
            .setMessage(R.string.seller_confirm_reject_message)
            .setPositiveButton(R.string.seller_action_reject) { _, _ ->
                updateStatus(order, OrderStatus.CANCELLED,
                    getString(R.string.seller_order_rejected))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun updateStatus(order: Order, newStatus: String, successMessage: String) {
        OrderRepository.updateOrderStatus(
            orderId = order.orderId,
            newStatus = newStatus,
            onSuccess = {
                if (_binding != null) {
                    Toast.makeText(requireContext(), successMessage,
                        Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        getString(R.string.seller_order_update_failed) + ": $errorMessage",
                        Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ordersListener?.remove()
        ordersListener = null
        _binding = null
    }
}