package com.example.foodorderapp.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.data.model.Order
import com.example.foodorderapp.data.model.OrderStatus
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.AdminRepository
import com.example.foodorderapp.databinding.ActivityAdminOrdersBinding
import com.example.foodorderapp.ui.admin.adapter.AdminOrderAdapter
import com.example.foodorderapp.ui.buyer.OrderDetailActivity
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.ListenerRegistration

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminOrdersBinding

    private lateinit var orderAdapter: AdminOrderAdapter
    private var allOrders: List<Order> = emptyList()
    private var currentTab: OrderTab = OrderTab.ALL
    private var currentSearchQuery: String = ""

    private val buyerNameMap = mutableMapOf<String, String>()

    private var ordersListener: ListenerRegistration? = null

    enum class OrderTab {
        ALL, PENDING, ACTIVE, COMPLETED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        setupTabs()
        setupSearch()
        startListeningToOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = AdminOrderAdapter(emptyList()) { order ->
            // Klik order → buka detail (mode buyer untuk read-only)
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.orderId)
            intent.putExtra(OrderDetailActivity.EXTRA_VIEW_MODE,
                OrderDetailActivity.MODE_BUYER)
            startActivity(intent)
        }

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(this@AdminOrdersActivity)
            adapter = orderAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = when (tab?.position) {
                    0 -> OrderTab.ALL
                    1 -> OrderTab.PENDING
                    2 -> OrderTab.ACTIVE
                    3 -> OrderTab.COMPLETED
                    else -> OrderTab.ALL
                }
                applyFilters()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString() ?: ""
                applyFilters()
            }
        })
    }

    private fun startListeningToOrders() {
        showLoading(true)

        ordersListener = AdminRepository.listenAllOrders(
            onUpdate = { orders ->
                showLoading(false)
                allOrders = orders

                // Pre-fetch buyer names untuk search efisien
                preloadBuyerNames(orders)

                applyFilters()
            },
            onError = { errorMessage ->
                showLoading(false)
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * Pre-load buyer names untuk efficient search.
     */
    private fun preloadBuyerNames(orders: List<Order>) {
        val uniqueBuyerIds = orders.map { it.buyerId }.distinct()

        for (buyerId in uniqueBuyerIds) {
            if (!buyerNameMap.containsKey(buyerId)) {
                FirebaseHelper.firestore
                    .collection(FirebaseHelper.COLLECTION_USERS)
                    .document(buyerId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("name") ?: "Unknown"
                        buyerNameMap[buyerId] = name
                        // Kalau ada search active, re-apply filter setelah names loaded
                        if (currentSearchQuery.isNotBlank()) {
                            applyFilters()
                        }
                    }
            }
        }
    }

    /**
     * Apply filter tab + search query.
     */
    private fun applyFilters() {
        // Step 1: filter by tab
        var filtered = filterByTab(allOrders, currentTab)

        // Step 2: filter by search
        filtered = AdminRepository.searchOrders(filtered, currentSearchQuery, buyerNameMap)

        orderAdapter.updateOrders(filtered)

        // Update count
        binding.tvOrderCount.text = "${allOrders.size} orders total"

        updateEmptyState(filtered.isEmpty())
    }

    private fun filterByTab(orders: List<Order>, tab: OrderTab): List<Order> {
        return when (tab) {
            OrderTab.ALL -> orders
            OrderTab.PENDING -> orders.filter { it.status == OrderStatus.PENDING }
            OrderTab.ACTIVE -> orders.filter {
                it.status == OrderStatus.ACCEPTED ||
                        it.status == OrderStatus.PREPARING ||
                        it.status == OrderStatus.READY ||
                        it.status == OrderStatus.SHIPPED
            }
            OrderTab.COMPLETED -> orders.filter {
                it.status == OrderStatus.DELIVERED ||
                        it.status == OrderStatus.CANCELLED
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        ordersListener?.remove()
        ordersListener = null
    }
}