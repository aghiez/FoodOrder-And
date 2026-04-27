package com.example.foodorderapp.ui.seller

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.repository.MenuRepository
import com.example.foodorderapp.databinding.FragmentMenuManagementBinding
import com.example.foodorderapp.ui.seller.adapter.MenuManagementAdapter
import com.google.firebase.firestore.ListenerRegistration

class MenuManagementFragment : Fragment() {

    private var _binding: FragmentMenuManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var menuAdapter: MenuManagementAdapter
    private var menus: List<Food> = emptyList()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        startListeningToMenus()
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuManagementAdapter(emptyList()) { food, anchorView ->
            showMenuOptions(food, anchorView)
        }

        binding.rvMenus.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = menuAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddMenu.setOnClickListener {
            // TODO: Akan dibuat di C.4
            //Toast.makeText(requireContext(),
            //    "Add Menu feature coming in next step (C.4)!",
            //    Toast.LENGTH_SHORT).show()
            // Sementara kita uncomment di C.4 saat AddEditMenuActivity sudah dibuat
            // startActivity(Intent(requireContext(), AddEditMenuActivity::class.java))
            startActivity(Intent(requireContext(), AddEditMenuActivity::class.java))

        }
    }

    private fun startListeningToMenus() {
        showLoading(true)

        listenerRegistration = MenuRepository.listenToSellerMenus(
            onUpdate = { menuList ->
                if (_binding == null) return@listenToSellerMenus

                showLoading(false)
                menus = menuList
                menuAdapter.updateMenus(menuList)
                updateUI()
            },
            onError = { errorMessage ->
                if (_binding == null) return@listenToSellerMenus

                showLoading(false)
                Toast.makeText(requireContext(),
                    "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateUI() {
        val isEmpty = menus.isEmpty()

        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvMenus.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (!isEmpty) {
            binding.tvMenuCount.text = getString(R.string.menu_mgmt_count, menus.size)
        } else {
            binding.tvMenuCount.text = ""
        }
    }

    private fun showMenuOptions(food: Food, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)

        // Toggle availability label
        val toggleLabel = if (food.isAvailable)
            R.string.menu_mgmt_action_toggle_off
        else R.string.menu_mgmt_action_toggle_on

        popup.menu.add(0, 1, 0, getString(toggleLabel))
        popup.menu.add(0, 2, 1, getString(R.string.menu_mgmt_action_edit))
        popup.menu.add(0, 3, 2, getString(R.string.menu_mgmt_action_delete))

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> toggleAvailability(food)
                2 -> editMenu(food)
                3 -> showDeleteConfirmation(food)
            }
            true
        }

        popup.show()
    }

    private fun toggleAvailability(food: Food) {
        MenuRepository.toggleAvailability(
            foodId = food.id,
            newAvailability = !food.isAvailable,
            onSuccess = {
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        getString(R.string.menu_mgmt_status_updated),
                        Toast.LENGTH_SHORT).show()
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

    private fun editMenu(food: Food) {
        // TODO: Akan dibuat di C.4
        //Toast.makeText(requireContext(),
        //    "Edit feature coming in C.4: ${food.name}",
        //    Toast.LENGTH_SHORT).show()
        // Sementara kita uncomment di C.4
         val intent = Intent(requireContext(), AddEditMenuActivity::class.java)
         intent.putExtra(AddEditMenuActivity.EXTRA_FOOD_ID, food.id)
         startActivity(intent)
    }

    private fun showDeleteConfirmation(food: Food) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.menu_mgmt_delete_confirm_title)
            .setMessage(getString(R.string.menu_mgmt_delete_confirm_message))
            .setPositiveButton(R.string.menu_mgmt_action_delete) { _, _ ->
                deleteMenu(food)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun deleteMenu(food: Food) {
        MenuRepository.deleteMenu(
            foodId = food.id,
            onSuccess = {
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        getString(R.string.menu_mgmt_delete_success),
                        Toast.LENGTH_SHORT).show()
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