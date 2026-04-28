package com.example.foodorderapp.ui.seller

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.foodorderapp.R
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.SellerRepository
import com.example.foodorderapp.databinding.FragmentStoreProfileBinding
import com.example.foodorderapp.ui.auth.LoginActivity
import com.example.foodorderapp.ui.buyer.ChangePasswordActivity
import com.example.foodorderapp.utils.Formatter
import com.google.firebase.firestore.ListenerRegistration

class StoreProfileFragment : Fragment() {

    private var _binding: FragmentStoreProfileBinding? = null
    private val binding get() = _binding!!

    private var profileListener: ListenerRegistration? = null

    // Flag untuk avoid trigger toggle saat set programmatically
    private var isInitialLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        startListeningToProfile()
        loadStoreStats()
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener { showLogoutConfirmation() }

        binding.llEditProfile.setOnClickListener {
            // TODO: Akan ke EditStoreProfileActivity di C.6.2
            Toast.makeText(requireContext(),
                "Edit Profile (akan ditambah di C.6.2)",
                Toast.LENGTH_SHORT).show()
        }

        binding.llChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        // Toggle store status (open/close)
        binding.switchStoreStatus.setOnCheckedChangeListener { _, isChecked ->
            // Skip kalau ini initial load (set programmatically)
            if (isInitialLoad) return@setOnCheckedChangeListener

            toggleStoreStatus(isChecked)
        }
    }

    private fun startListeningToProfile() {
        showLoading(true)

        profileListener = SellerRepository.listenToStoreProfile(
            onUpdate = { data ->
                if (_binding == null) return@listenToStoreProfile

                showLoading(false)
                displayProfile(data)
            },
            onError = { errorMessage ->
                if (_binding == null) return@listenToStoreProfile

                showLoading(false)
                Toast.makeText(requireContext(),
                    getString(R.string.store_load_failed) + ": $errorMessage",
                    Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun displayProfile(data: Map<String, Any>) {
        // Store name & email
        val storeName = data["storeName"] as? String ?: data["name"] as? String ?: "Store"
        val email = data["email"] as? String ?: ""

        binding.tvStoreName.text = storeName
        binding.tvStoreEmail.text = email

        // Avatar initial
        val initial = if (storeName.isNotEmpty()) {
            storeName.first().uppercase()
        } else {
            "S"
        }
        binding.tvStoreInitial.text = initial

        // Store info
        binding.tvStoreDescription.text = data["storeDescription"] as? String
            ?: "No description yet"
        binding.tvStoreAddress.text = data["storeAddress"] as? String
            ?: "No address yet"
        binding.tvStorePhone.text = data["phone"] as? String ?: "-"

        // Toggle store status
        val isOpen = data["isOpen"] as? Boolean ?: true
        updateStoreStatusUI(isOpen)
    }

    /**
     * Update tampilan toggle status (TANPA trigger listener).
     */
    private fun updateStoreStatusUI(isOpen: Boolean) {
        // Set isInitialLoad = true untuk skip listener
        isInitialLoad = true
        binding.switchStoreStatus.isChecked = isOpen
        isInitialLoad = false

        if (isOpen) {
            binding.tvStoreStatus.text = getString(R.string.store_status_open)
            binding.tvStoreStatusDesc.text = getString(R.string.store_status_open_desc)
            binding.viewStatusIndicator.backgroundTintList = ContextCompat
                .getColorStateList(requireContext(), R.color.success)
        } else {
            binding.tvStoreStatus.text = getString(R.string.store_status_closed)
            binding.tvStoreStatusDesc.text = getString(R.string.store_status_closed_desc)
            binding.viewStatusIndicator.backgroundTintList = ContextCompat
                .getColorStateList(requireContext(), R.color.error)
        }
    }

    private fun toggleStoreStatus(isOpen: Boolean) {
        SellerRepository.toggleStoreStatus(
            isOpen = isOpen,
            onSuccess = {
                if (_binding != null) {
                    val message = if (isOpen) {
                        getString(R.string.store_toggle_open_success)
                    } else {
                        getString(R.string.store_toggle_close_success)
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        getString(R.string.store_toggle_failed) + ": $errorMessage",
                        Toast.LENGTH_SHORT).show()
                    // Revert toggle ke state sebelumnya
                    isInitialLoad = true
                    binding.switchStoreStatus.isChecked = !isOpen
                    isInitialLoad = false
                }
            }
        )
    }

    private fun loadStoreStats() {
        SellerRepository.calculateStoreStats(
            onSuccess = { totalRevenue, totalOrders ->
                if (_binding == null) return@calculateStoreStats

                binding.tvTotalRevenue.text = Formatter.toRupiah(totalRevenue)
                binding.tvTotalOrders.text = "$totalOrders orders"

                // Rating: untuk sekarang hardcoded, akan implement di Tahap 6
                binding.tvRating.text = "⭐ - (no reviews yet)"
            },
            onFailure = { _ ->
                if (_binding == null) return@calculateStoreStats

                binding.tvTotalRevenue.text = "Rp 0"
                binding.tvTotalOrders.text = "0 orders"
                binding.tvRating.text = "⭐ -"
            }
        )
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.store_logout_title)
            .setMessage(R.string.store_logout_message)
            .setPositiveButton(R.string.store_action_logout) { _, _ ->
                doLogout()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun doLogout() {
        FirebaseHelper.auth.signOut()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileListener?.remove()
        profileListener = null
        _binding = null
    }
}