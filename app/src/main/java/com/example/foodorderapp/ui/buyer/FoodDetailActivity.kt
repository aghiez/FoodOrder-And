package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.model.Review
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.CartRepository
import com.example.foodorderapp.data.repository.ReviewRepository
import com.example.foodorderapp.databinding.ActivityFoodDetailBinding
import com.example.foodorderapp.ui.buyer.adapter.ReviewAdapter
import com.example.foodorderapp.utils.ActivityTransitionHelper
import com.example.foodorderapp.utils.Formatter
import com.google.firebase.firestore.ListenerRegistration

class FoodDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoodDetailBinding
    private lateinit var food: Food
    private var sellerName: String = ""
    private var quantity: Int = 1

    private lateinit var reviewAdapter: ReviewAdapter
    private var reviewsListener: ListenerRegistration? = null

    companion object {
        const val EXTRA_FOOD = "extra_food"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val foodExtra = intent.getParcelableExtra<Food>(EXTRA_FOOD)
        if (foodExtra == null) {
            Toast.makeText(this, "Food data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        food = foodExtra

        displayFoodData()
        loadSellerName()
        setupListeners()
        updateUI()
        setupReviewsRecyclerView()
        startListeningToReviews()
    }

    private fun displayFoodData() {
        binding.tvFoodName.text = food.name
        binding.tvFoodDescription.text = food.description
        binding.tvFoodPrice.text = Formatter.toRupiah(food.price)

        Glide.with(this)
            .load(food.imageUrl)
            .placeholder(R.drawable.ic_food)
            .error(R.drawable.ic_food)
            .into(binding.ivFoodImage)

        // Stats row
        displayRating(food.rating, food.totalReviews)
        binding.tvDetailSold.text = "${food.totalSold} sold"
        binding.tvDetailStock.text = "Stock: ${food.stock}"
    }

    private fun displayRating(rating: Double, totalReviews: Int) {
        if (totalReviews > 0) {
            binding.tvDetailRating.text = String.format("%.1f", rating)
            binding.tvDetailReviewCount.text = "($totalReviews reviews)"
        } else {
            binding.tvDetailRating.text = "-"
            binding.tvDetailReviewCount.text = "(No reviews yet)"
        }
    }

    private fun loadSellerName() {
        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_SELLERS)
            .document(food.sellerId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    sellerName = document.getString("storeName") ?: "Unknown Store"
                    binding.tvSellerName.text = sellerName
                }
            }
            .addOnFailureListener {
                binding.tvSellerName.text = "Unknown Store"
            }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
            ActivityTransitionHelper.slideDown(this)
        }

        binding.btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateUI()
            }
        }

        binding.btnIncrease.setOnClickListener {
            if (quantity < food.stock) {
                quantity++
                updateUI()
            } else {
                Toast.makeText(this, "Maximum stock reached", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddToCart.setOnClickListener { addToCart() }
    }

    private fun updateUI() {
        binding.tvQuantity.text = quantity.toString()

        val total = food.price * quantity
        binding.btnAddToCart.text = "Add to Cart - ${Formatter.toRupiah(total)}"

        if (food.stock == 0 || !food.isAvailable) {
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = getString(R.string.detail_unavailable)
            binding.btnDecrease.isEnabled = false
            binding.btnIncrease.isEnabled = false
        }
    }

    private fun addToCart() {
        if (sellerName.isEmpty()) {
            Toast.makeText(this, "Please wait, loading seller info…",
                Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAddToCart.isEnabled = false
        binding.btnAddToCart.text = "Adding..."

        CartRepository.addToCart(
            food = food,
            sellerName = sellerName,
            quantity = quantity,
            onSuccess = {
                Toast.makeText(this, getString(R.string.cart_added_to_cart),
                    Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { errorMessage ->
                binding.btnAddToCart.isEnabled = true
                updateUI()
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setupReviewsRecyclerView() {
        reviewAdapter = ReviewAdapter(emptyList())
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(this@FoodDetailActivity)
            adapter = reviewAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun startListeningToReviews() {
        reviewsListener = ReviewRepository.listenReviewsForFood(
            foodId = food.id,
            onUpdate = { reviews ->
                reviewAdapter.updateReviews(reviews)
                updateReviewsUI(reviews)
            },
            onError = { _ ->
                updateReviewsUI(emptyList())
            }
        )
    }

    private fun updateReviewsUI(reviews: List<Review>) {
        if (reviews.isEmpty()) {
            binding.rvReviews.visibility = View.GONE
            binding.tvNoReviews.visibility = View.VISIBLE
            binding.tvNoReviewsSubtitle.visibility = View.VISIBLE
            binding.tvReviewSummary.text = "0 reviews"
        } else {
            binding.rvReviews.visibility = View.VISIBLE
            binding.tvNoReviews.visibility = View.GONE
            binding.tvNoReviewsSubtitle.visibility = View.GONE
            binding.tvReviewSummary.text = if (reviews.size == 1) {
                "1 review"
            } else {
                "${reviews.size} reviews"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reviewsListener?.remove()
        reviewsListener = null
    }
}