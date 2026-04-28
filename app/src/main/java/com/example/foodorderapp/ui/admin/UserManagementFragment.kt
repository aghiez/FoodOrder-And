package com.example.foodorderapp.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.R
import com.example.foodorderapp.data.repository.AdminRepository
import com.example.foodorderapp.databinding.FragmentAdminUsersBinding
import com.example.foodorderapp.ui.admin.adapter.UserManagementAdapter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.ListenerRegistration

class UserManagementFragment : Fragment() {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!

    private lateinit var userAdapter: UserManagementAdapter
    private var allUsers: List<Map<String, Any>> = emptyList()
    private var currentTab: AdminRepository.UserTab = AdminRepository.UserTab.ALL
    private var currentSearchQuery: String = ""

    private var usersListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        setupSearch()
        startListeningToUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserManagementAdapter(emptyList()) { user ->
            // Klik user → buka detail
            val intent = Intent(requireContext(), UserDetailActivity::class.java)
            intent.putExtra(UserDetailActivity.EXTRA_USER_ID,
                user["userId"] as? String ?: "")
            startActivity(intent)
        }

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = when (tab?.position) {
                    0 -> AdminRepository.UserTab.ALL
                    1 -> AdminRepository.UserTab.BUYERS
                    2 -> AdminRepository.UserTab.SELLERS
                    3 -> AdminRepository.UserTab.ADMINS
                    else -> AdminRepository.UserTab.ALL
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

    private fun startListeningToUsers() {
        showLoading(true)

        usersListener = AdminRepository.listenAllUsers(
            onUpdate = { users ->
                if (_binding == null) return@listenAllUsers

                showLoading(false)
                allUsers = users
                applyFilters()
            },
            onError = { errorMessage ->
                if (_binding != null) {
                    showLoading(false)
                    Toast.makeText(requireContext(),
                        "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    /**
     * Apply filter tab + search query.
     */
    private fun applyFilters() {
        if (_binding == null) return

        // Step 1: filter by tab
        var filtered = AdminRepository.filterUsersByTab(allUsers, currentTab)

        // Step 2: filter by search query
        filtered = AdminRepository.searchUsers(filtered, currentSearchQuery)

        userAdapter.updateUsers(filtered)

        // Update count
        binding.tvUserCount.text = getString(R.string.users_count, allUsers.size)

        updateEmptyState(filtered.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvUsers.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersListener?.remove()
        usersListener = null
        _binding = null
    }
}