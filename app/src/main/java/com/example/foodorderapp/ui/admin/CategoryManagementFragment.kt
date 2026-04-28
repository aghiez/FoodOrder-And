package com.example.foodorderapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Category
import com.example.foodorderapp.data.repository.AdminRepository
import com.example.foodorderapp.databinding.FragmentAdminCategoriesBinding
import com.example.foodorderapp.ui.admin.adapter.CategoryManagementAdapter
import com.google.firebase.firestore.ListenerRegistration

class CategoryManagementFragment : Fragment() {

    private var _binding: FragmentAdminCategoriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryManagementAdapter
    private var allCategories: List<Category> = emptyList()

    private var categoriesListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        startListeningToCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryManagementAdapter(
            categories = emptyList(),
            onEdit = { category -> showAddEditDialog(category) },
            onDelete = { category -> showDeleteConfirmation(category) }
        )

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddCategory.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun startListeningToCategories() {
        showLoading(true)

        categoriesListener = AdminRepository.listenAllCategories(
            onUpdate = { categories ->
                if (_binding == null) return@listenAllCategories

                showLoading(false)
                allCategories = categories
                categoryAdapter.updateCategories(categories)

                binding.tvCategoryCount.text = getString(R.string.categories_count,
                    categories.size)

                updateEmptyState(categories.isEmpty())
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
     * Tampilkan dialog Add atau Edit category.
     */
    private fun showAddEditDialog(existingCategory: Category?) {
        val isEditMode = existingCategory != null

        // Inflate custom layout
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_edit_category, null)

        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val etOrder = dialogView.findViewById<EditText>(R.id.etCategoryOrder)

        // Pre-fill kalau edit
        if (existingCategory != null) {
            etName.setText(existingCategory.name)
            etOrder.setText(existingCategory.order.toString())
        } else {
            // Suggest next order number
            val nextOrder = (allCategories.maxOfOrNull { it.order } ?: 0) + 1
            etOrder.setText(nextOrder.toString())
        }

        val title = if (isEditMode) {
            getString(R.string.category_form_edit_title)
        } else {
            getString(R.string.category_form_add_title)
        }

        val positiveButtonText = if (isEditMode) {
            getString(R.string.category_form_update)
        } else {
            getString(R.string.category_form_save)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(positiveButtonText, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        // Override positive button click untuk validation
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                handleSave(existingCategory, etName, etOrder, dialog)
            }
        }

        dialog.show()
    }

    /**
     * Validasi dan save category.
     */
    private fun handleSave(
        existingCategory: Category?,
        etName: EditText,
        etOrder: EditText,
        dialog: AlertDialog
    ) {
        val name = etName.text.toString().trim()
        val orderStr = etOrder.text.toString().trim()

        // Validation: name
        if (name.isEmpty()) {
            etName.error = getString(R.string.category_error_name_required)
            return
        }
        if (name.length < 2) {
            etName.error = getString(R.string.category_error_name_short)
            return
        }

        // Validation: order
        if (orderStr.isEmpty()) {
            etOrder.error = getString(R.string.category_error_order_required)
            return
        }
        val order = orderStr.toIntOrNull()
        if (order == null || order < 1) {
            etOrder.error = getString(R.string.category_error_order_invalid)
            return
        }

        // Cek duplicate name
        AdminRepository.checkCategoryNameExists(
            name = name,
            excludeId = existingCategory?.id,
            onResult = { exists ->
                if (_binding == null) return@checkCategoryNameExists

                if (exists) {
                    etName.error = getString(R.string.category_error_name_exists)
                    return@checkCategoryNameExists
                }

                // Save
                if (existingCategory != null) {
                    updateCategory(existingCategory.id, name, order, dialog)
                } else {
                    addCategory(name, order, dialog)
                }
            }
        )
    }

    private fun addCategory(name: String, order: Int, dialog: AlertDialog) {
        AdminRepository.addCategory(
            name = name,
            order = order,
            onSuccess = {
                Toast.makeText(requireContext(),
                    getString(R.string.category_added_success),
                    Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            },
            onFailure = { errorMessage ->
                Toast.makeText(requireContext(),
                    getString(R.string.category_action_failed) + ": $errorMessage",
                    Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun updateCategory(
        categoryId: String,
        name: String,
        order: Int,
        dialog: AlertDialog
    ) {
        AdminRepository.updateCategory(
            categoryId = categoryId,
            name = name,
            order = order,
            onSuccess = {
                Toast.makeText(requireContext(),
                    getString(R.string.category_updated_success),
                    Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            },
            onFailure = { errorMessage ->
                Toast.makeText(requireContext(),
                    getString(R.string.category_action_failed) + ": $errorMessage",
                    Toast.LENGTH_LONG).show()
            }
        )
    }

    /**
     * Confirm delete category dengan check apakah ada menu yang pakai.
     */
    private fun showDeleteConfirmation(category: Category) {
        // Cek dulu berapa menu yang pakai kategori ini
        AdminRepository.countMenusInCategory(
            categoryId = category.id,
            onSuccess = { count ->
                if (_binding == null) return@countMenusInCategory

                val message = if (count > 0) {
                    getString(R.string.category_delete_message_with_menus, count)
                } else {
                    getString(R.string.category_delete_message)
                }

                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.category_delete_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.category_delete_confirm) { _, _ ->
                        deleteCategory(category)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            },
            onFailure = {
                if (_binding == null) return@countMenusInCategory

                // Kalau gagal cek, tetap kasih warning umum
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.category_delete_title)
                    .setMessage(R.string.category_delete_message)
                    .setPositiveButton(R.string.category_delete_confirm) { _, _ ->
                        deleteCategory(category)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }
        )
    }

    private fun deleteCategory(category: Category) {
        AdminRepository.deleteCategory(
            categoryId = category.id,
            onSuccess = {
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        getString(R.string.category_deleted_success),
                        Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { errorMessage ->
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        getString(R.string.category_action_failed) + ": $errorMessage",
                        Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvCategories.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        categoriesListener?.remove()
        categoriesListener = null
        _binding = null
    }
}