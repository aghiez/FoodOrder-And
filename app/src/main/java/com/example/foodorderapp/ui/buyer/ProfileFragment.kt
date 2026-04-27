package com.example.foodorderapp.ui.buyer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.User
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.UserRepository
import com.example.foodorderapp.databinding.FragmentProfileBinding
import com.example.foodorderapp.ui.auth.LoginActivity
import com.example.foodorderapp.utils.Formatter

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Reload data setiap kali fragment muncul (setelah edit profile)
        loadUserProfile()
        loadUserStatistics()
    }

    private fun setupClickListeners() {
        binding.menuEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.menuChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        binding.menuAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.menuLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserProfile() {
        UserRepository.getCurrentUser(
            onSuccess = { user ->
                if (_binding != null) {
                    displayUserData(user)
                }
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun displayUserData(user: User) {
        binding.tvUserName.text = user.name
        binding.tvUserEmail.text = user.email
        binding.tvAvatarInitials.text = Formatter.getInitials(user.name)
        binding.tvMemberSince.text = "Member since ${Formatter.toMonthYear(user.createdAt)}"
    }

    private fun loadUserStatistics() {
        UserRepository.getUserStatistics(
            onSuccess = { totalOrders, totalSpent ->
                if (_binding != null) {
                    binding.tvStatOrders.text = totalOrders.toString()
                    binding.tvStatSpent.text = Formatter.toRupiahShort(totalSpent)
                }
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    binding.tvStatOrders.text = "0"
                    binding.tvStatSpent.text = "Rp 0"
                }
            }
        )
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_about_title)
            .setMessage(R.string.profile_about_message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_logout_confirm_title)
            .setMessage(R.string.profile_logout_confirm_message)
            .setPositiveButton(R.string.profile_menu_logout) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun performLogout() {
        FirebaseHelper.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}