package com.example.foodorderapp.ui.seller

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.databinding.FragmentStoreProfileBinding
import com.example.foodorderapp.ui.auth.LoginActivity

class StoreProfileFragment : Fragment() {

    private var _binding: FragmentStoreProfileBinding? = null
    private val binding get() = _binding!!

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

        loadStoreInfo()

        binding.btnSellerLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun loadStoreInfo() {
        val userId = FirebaseHelper.getCurrentUserId() ?: return

        // Load user data
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && _binding != null) {
                    binding.tvSellerEmail.text = document.getString("email") ?: ""
                }
            }

        // Load seller (store) data
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_SELLERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && _binding != null) {
                    binding.tvStoreName.text = document.getString("storeName") ?: "My Store"
                }
            }
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