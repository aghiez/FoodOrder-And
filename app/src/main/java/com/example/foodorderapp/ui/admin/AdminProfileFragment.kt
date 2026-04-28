package com.example.foodorderapp.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.foodorderapp.R
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.AdminRepository
import com.example.foodorderapp.databinding.FragmentAdminProfileBinding
import com.example.foodorderapp.ui.auth.LoginActivity
import com.example.foodorderapp.ui.buyer.ChangePasswordActivity
import com.google.firebase.firestore.ListenerRegistration

class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!

    private var profileListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        startListeningToProfile()
        loadProfileStats()
    }

    private fun setupListeners() {
        // Quick Actions
        binding.llManageUsers.setOnClickListener {
            // Navigate ke tab Users dengan trigger Bottom Nav
            val activity = requireActivity() as? AdminDashboardActivity
            activity?.navigateToTab(R.id.nav_admin_users)
        }

        binding.llManageCategories.setOnClickListener {
            val activity = requireActivity() as? AdminDashboardActivity
            activity?.navigateToTab(R.id.nav_admin_categories)
        }

        binding.llViewAllOrders.setOnClickListener {
            startActivity(Intent(requireContext(), AdminOrdersActivity::class.java))
        }

        // Account
        binding.llEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditAdminProfileActivity::class.java))
        }

        binding.llChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun startListeningToProfile() {
        profileListener = AdminRepository.listenAdminProfile(
            onUpdate = { data ->
                if (_binding == null) return@listenAdminProfile
                displayProfile(data)
            },
            onError = { errorMessage ->
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        "Error loading profile: $errorMessage",
                        Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun displayProfile(data: Map<String, Any>) {
        val name = data["name"] as? String ?: "Admin"
        val email = data["email"] as? String ?: ""

        binding.tvAdminName.text = name
        binding.tvAdminEmail.text = email
        binding.tvAdminInitial.text = name.firstOrNull()?.uppercase() ?: "A"
    }

    private fun loadProfileStats() {
        AdminRepository.getProfileStats(
            onSuccess = { totalUsers, totalOrders ->
                if (_binding == null) return@getProfileStats

                binding.tvStatTotalUsers.text = totalUsers.toString()
                binding.tvStatTotalOrders.text = totalOrders.toString()
            },
            onFailure = {
                if (_binding != null) {
                    binding.tvStatTotalUsers.text = "0"
                    binding.tvStatTotalOrders.text = "0"
                }
            }
        )
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.admin_logout_title)
            .setMessage(R.string.admin_logout_message)
            .setPositiveButton(R.string.admin_profile_logout) { _, _ ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        profileListener?.remove()
        profileListener = null
        _binding = null
    }
}