package com.example.foodorderapp.ui.buyer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.data.model.Category
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.databinding.FragmentHomeBinding
import com.example.foodorderapp.ui.buyer.adapter.CategoryAdapter
import com.example.foodorderapp.ui.buyer.adapter.FoodAdapter
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val TAG = "HomeFragment"

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var foodAdapter: FoodAdapter

    private val categories = mutableListOf<Category>()
    private var allFoods = listOf<Food>()
    private var filteredFoods = listOf<Food>()
    private var selectedCategoryId: String = "all"

    // Real-time listeners
    private var foodsListener: ListenerRegistration? = null
    private var categoriesListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserName()
        setupRecyclerViews()
        startListeningToCategories()
        startListeningToFoods()
    }

    private fun loadUserName() {
        val userId = FirebaseHelper.getCurrentUserId() ?: return

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && _binding != null) {
                    binding.tvUserName.text = document.getString("name") ?: "User"
                }
            }
    }

    private fun setupRecyclerViews() {
        // Category RecyclerView
        categoryAdapter = CategoryAdapter(categories) { category ->
            selectedCategoryId = category.id
            filterFoodsByCategory(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Food RecyclerView
        foodAdapter = FoodAdapter(emptyList()) { food ->
            val intent = Intent(requireContext(), FoodDetailActivity::class.java)
            intent.putExtra(FoodDetailActivity.EXTRA_FOOD, food)
            startActivity(intent)
        }
        binding.rvFoods.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = foodAdapter
            isNestedScrollingEnabled = false
        }
    }

    /**
     * Listen real-time changes ke categories.
     */
    private fun startListeningToCategories() {
        categoriesListener = FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CATEGORIES)
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener

                if (error != null) {
                    Log.e(TAG, "Error loading categories", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Reset & re-add "All" + categories from Firestore
                    categories.clear()
                    categories.add(Category(id = "all", name = "All", order = 0))

                    for (doc in snapshot.documents) {
                        try {
                            val category = doc.toObject(Category::class.java)
                                ?.copy(id = doc.id)
                            if (category != null) {
                                categories.add(category)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing category: ${doc.id}", e)
                        }
                    }

                    categoryAdapter.notifyDataSetChanged()
                    Log.d(TAG, "Loaded ${categories.size - 1} categories from Firestore")
                }
            }
    }

    /**
     * Listen real-time changes ke foods.
     */
    private fun startListeningToFoods() {
        showLoading(true)

        foodsListener = FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener

                showLoading(false)

                if (error != null) {
                    Log.e(TAG, "Error loading foods", error)
                    Toast.makeText(requireContext(),
                        "Failed to load foods: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val foodList = mutableListOf<Food>()
                    for (doc in snapshot.documents) {
                        try {
                            val food = doc.toObject(Food::class.java)
                                ?.copy(id = doc.id)
                            if (food != null) {
                                foodList.add(food)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing food: ${doc.id}", e)
                        }
                    }

                    allFoods = foodList

                    // Re-apply current filter
                    val currentCategory = categories.find { it.id == selectedCategoryId }
                    if (currentCategory != null) {
                        filterFoodsByCategory(currentCategory)
                    } else {
                        filteredFoods = foodList
                        foodAdapter.updateFoods(filteredFoods)
                        showEmptyState(foodList.isEmpty())
                    }

                    Log.d(TAG, "Loaded ${foodList.size} foods from Firestore")
                }
            }
    }

    private fun filterFoodsByCategory(category: Category) {
        selectedCategoryId = category.id

        filteredFoods = if (category.id == "all") {
            allFoods
        } else {
            allFoods.filter { it.categoryId == category.id }
        }
        foodAdapter.updateFoods(filteredFoods)
        showEmptyState(filteredFoods.isEmpty())
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFoods.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        binding.tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFoods.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // PENTING: Remove listeners untuk cegah memory leak
        foodsListener?.remove()
        foodsListener = null
        categoriesListener?.remove()
        categoriesListener = null
        _binding = null
    }
}