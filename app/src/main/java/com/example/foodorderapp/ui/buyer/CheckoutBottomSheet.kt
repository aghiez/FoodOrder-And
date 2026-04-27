package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.CartItem
import com.example.foodorderapp.data.repository.OrderRepository
import com.example.foodorderapp.databinding.BottomSheetCheckoutBinding
import com.example.foodorderapp.utils.Formatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CheckoutBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCheckoutBinding? = null
    private val binding get() = _binding!!

    private var cartItems: List<CartItem> = emptyList()
    private val deliveryFee = 5000.0

    var onOrderPlaced: ((orderId: String) -> Unit)? = null

    companion object {
        const val TAG = "CheckoutBottomSheet"

        fun newInstance(items: List<CartItem>): CheckoutBottomSheet {
            return CheckoutBottomSheet().apply {
                cartItems = items
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displaySummary()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()

        // Expand bottom sheet penuh agar tombol Place Order terlihat
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet)

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true

            // Set tinggi 90% dari layar
            val displayMetrics = resources.displayMetrics
            it.layoutParams.height = (displayMetrics.heightPixels * 0.9).toInt()
            it.requestLayout()
        }
    }

    private fun displaySummary() {
        val subtotal = cartItems.sumOf { it.subtotal }
        val total = subtotal + deliveryFee

        binding.tvSubtotal.text = Formatter.toRupiah(subtotal)
        binding.tvDeliveryFee.text = Formatter.toRupiah(deliveryFee)
        binding.tvTotal.text = Formatter.toRupiah(total)
    }

    private fun setupListeners() {
        binding.btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
    }

    private fun placeOrder() {
        val address = binding.etAddress.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        if (address.isEmpty()) {
            binding.etAddress.error = "Address is required"
            binding.etAddress.requestFocus()
            return
        }

        if (address.length < 10) {
            binding.etAddress.error = "Address too short"
            binding.etAddress.requestFocus()
            return
        }

        val paymentMethod = when (binding.rgPayment.checkedRadioButtonId) {
            R.id.rbCash -> "Cash on Delivery"
            R.id.rbTransfer -> "Bank Transfer"
            else -> "Cash on Delivery"
        }

        showLoading(true)

        OrderRepository.createOrder(
            cartItems = cartItems,
            deliveryAddress = address,
            notes = notes,
            paymentMethod = paymentMethod,
            onSuccess = { orderId ->
                if (_binding == null) return@createOrder

                showLoading(false)
                Toast.makeText(requireContext(),
                    getString(R.string.checkout_success), Toast.LENGTH_SHORT).show()
                onOrderPlaced?.invoke(orderId)
                dismiss()
            },
            onFailure = { errorMessage ->
                if (_binding == null) return@createOrder

                showLoading(false)
                Toast.makeText(requireContext(),
                    "Failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnPlaceOrder.isEnabled = !show
        binding.btnPlaceOrder.text = if (show) "Processing..."
        else getString(R.string.checkout_place_order)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}