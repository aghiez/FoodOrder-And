package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodorderapp.data.model.Category
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.databinding.FragmentHomeBinding
import com.example.foodorderapp.ui.buyer.adapter.CategoryAdapter
import com.example.foodorderapp.ui.buyer.adapter.FoodAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val TAG = "HomeFragment"

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var foodAdapter: FoodAdapter

    private val categories = mutableListOf<Category>()
    private var allFoods = listOf<Food>()  // semua makanan dari Firestore
    private var filteredFoods = listOf<Food>()  // makanan setelah filter kategori

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
        loadCategories()
        loadFoods()
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
        // Category RecyclerView - horizontal
        categoryAdapter = CategoryAdapter(categories) { category ->
            filterFoodsByCategory(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Food RecyclerView - vertical
        foodAdapter = FoodAdapter(emptyList()) { food ->
            val intent = Intent(requireContext(), FoodDetailActivity::class.java)
            intent.putExtra(FoodDetailActivity.EXTRA_FOOD, food)
            startActivity(intent)
        }

        binding.rvFoods.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = foodAdapter
            isNestedScrollingEnabled = false  // disable scroll dalam NestedScrollView
        }
    }

    private fun loadCategories() {
        // Tambah "All" sebagai kategori pertama
        categories.clear()
        categories.add(Category(id = "all", name = "All", order = 0))

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_CATEGORIES)
            .orderBy("order")
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener

                for (doc in documents) {
                    val category = doc.toObject(Category::class.java)
                        .copy(id = doc.id)
                    categories.add(category)
                }
                categoryAdapter.notifyDataSetChanged()
                Log.d(TAG, "Loaded ${categories.size - 1} categories")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading categories", e)
                if (_binding != null) {
                    Toast.makeText(requireContext(),
                        "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadFoods() {
        showLoading(true)

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_FOODS)
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener

                showLoading(false)

                val foodList = mutableListOf<Food>()
                for (doc in documents) {
                    try {
                        val food = doc.toObject(Food::class.java)
                            .copy(id = doc.id)
                        foodList.add(food)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing food: ${doc.id}", e)
                    }
                }

                allFoods = foodList
                filteredFoods = foodList
                foodAdapter.updateFoods(filteredFoods)

                showEmptyState(foodList.isEmpty())
                Log.d(TAG, "Loaded ${foodList.size} foods")
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener

                showLoading(false)
                showEmptyState(true)
                Log.e(TAG, "Error loading foods", e)
                Toast.makeText(requireContext(),
                    "Failed to load foods: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterFoodsByCategory(category: Category) {
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
        _binding = null
    }
}