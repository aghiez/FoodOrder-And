package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.CartItem
import com.example.foodorderapp.data.repository.CartRepository
import com.example.foodorderapp.databinding.FragmentCartBinding
import com.example.foodorderapp.ui.buyer.adapter.CartAdapter
import com.example.foodorderapp.utils.Formatter
import com.google.firebase.firestore.ListenerRegistration

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var cartAdapter: CartAdapter
    private var cartItems: List<CartItem> = emptyList()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        startListeningToCart()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            items = emptyList(),
            onIncrease = { item -> updateQuantity(item, item.quantity + 1) },
            onDecrease = { item ->
                if (item.quantity > 1) {
                    updateQuantity(item, item.quantity - 1)
                } else {
                    showRemoveConfirmation(item)
                }
            },
            onRemove = { item -> showRemoveConfirmation(item) }
        )

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun setupListeners() {
        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmation()
        }

        binding.btnCheckout.setOnClickListener {
            if(cartItems.isNotEmpty()){
                showCheckoutBottomSheet()
            }
        }
    }

    private fun showCheckoutBottomSheet() {
        val bottomSheet = CheckoutBottomSheet.newInstance(cartItems)
        bottomSheet.onOrderPlaced = { orderId ->
            // Switch ke tab Orders setelah order berhasil
            (activity as? BuyerDashboardActivity)?.switchToOrdersTab()
        }
        bottomSheet.show(parentFragmentManager, CheckoutBottomSheet.TAG)
    }

    private fun startListeningToCart() {
        showLoading(true)

        listenerRegistration = CartRepository.listenToCartItems(
            onUpdate = { items ->
                if (_binding == null) return@listenToCartItems

                showLoading(false)
                cartItems = items
                cartAdapter.updateItems(items)
                updateUI()
            },
            onError = { errorMessage ->
                if (_binding == null) return@listenToCartItems

                showLoading(false)
                Toast.makeText(requireContext(),
                    "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateUI() {
        val isEmpty = cartItems.isEmpty()

        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvCartItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.checkoutLayout.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.btnClearAll.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (!isEmpty) {
            binding.tvItemCount.text = getString(R.string.cart_item_count, cartItems.size)
            binding.tvTotalPrice.text = Formatter.toRupiah(
                CartRepository.calculateTotal(cartItems))
        } else {
            binding.tvItemCount.text = ""
        }
    }

    private fun updateQuantity(item: CartItem, newQuantity: Int) {
        CartRepository.updateQuantity(
            foodId = item.foodId,
            newQuantity = newQuantity,
            onSuccess = {
                // Real-time listener akan auto-update UI
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        "Failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showRemoveConfirmation(item: CartItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cart_remove_confirm_title)
            .setMessage(R.string.cart_remove_confirm_message)
            .setPositiveButton(R.string.action_remove) { _, _ ->
                removeItem(item)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun removeItem(item: CartItem) {
        CartRepository.removeFromCart(
            foodId = item.foodId,
            onSuccess = {
                // Real-time listener akan auto-update UI
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        "Failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cart_clear_confirm_title)
            .setMessage(R.string.cart_clear_confirm_message)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                clearAll()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun clearAll() {
        CartRepository.clearCart(
            onSuccess = {
                // Real-time listener akan auto-update UI
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        "Failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
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