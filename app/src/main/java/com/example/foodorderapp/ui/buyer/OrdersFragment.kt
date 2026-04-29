package com.example.foodorderapp.ui.buyer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.FragmentOrdersBinding
import com.example.foodorderapp.ui.buyer.adapter.OrderAdapter
import com.google.firebase.firestore.ListenerRegistration
import com.example.foodorderapp.ui.buyer.WriteReviewActivity

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private lateinit var orderAdapter: OrderAdapter
    private var orders: List<Order> = emptyList()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        startListeningToOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            orders = emptyList(),
            onOrderClick = { order ->
                // Existing: buka detail
                val intent = Intent(requireContext(), OrderDetailActivity::class.java)
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.orderId)
                startActivity(intent)
            },
            onWriteReviewClick = { order ->
                // BARU: buka WriteReviewActivity
                handleWriteReview(order)
            }
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    /**
     * Handle write review click. Pakai item pertama dari order.
     */
    private fun handleWriteReview(order: Order) {
        if (order.items.isEmpty()) {
            Toast.makeText(requireContext(),
                "No items to review", Toast.LENGTH_SHORT).show()
            return
        }

        val firstItem = order.items.first()

        val intent = Intent(requireContext(), WriteReviewActivity::class.java)
        intent.putExtra(WriteReviewActivity.EXTRA_ORDER_ID, order.orderId)
        intent.putExtra(WriteReviewActivity.EXTRA_FOOD_ID, firstItem.foodId)
        intent.putExtra(WriteReviewActivity.EXTRA_FOOD_NAME, firstItem.foodName)
        intent.putExtra(WriteReviewActivity.EXTRA_SELLER_ID, order.sellerId)
        startActivity(intent)
    }

    private fun startListeningToOrders() {
        showLoading(true)

        listenerRegistration = OrderRepository.listenToBuyerOrders(
            onUpdate = { orderList ->
                if (_binding == null) return@listenToBuyerOrders

                showLoading(false)
                orders = orderList
                orderAdapter.updateOrders(orderList)
                updateUI()
            },
            onError = { errorMessage ->
                if (_binding == null) return@listenToBuyerOrders

                showLoading(false)
                Toast.makeText(requireContext(),
                    "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateUI() {
        val isEmpty = orders.isEmpty()

        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (!isEmpty) {
            binding.tvOrderCount.text = "${orders.size} orders"
        } else {
            binding.tvOrderCount.text = ""
        }
    }

    private fun navigateToOrderDetail(order: Order) {
        val intent = Intent(requireContext(), OrderDetailActivity::class.java)
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.orderId)
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        listenerRegistration = null
        _binding = null
    }
}