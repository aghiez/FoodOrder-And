package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorderapp.R
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.ReviewRepository
import com.example.foodorderapp.databinding.ActivityWriteReviewBinding

class WriteReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWriteReviewBinding

    // Data dari intent
    private var orderId: String = ""
    private var foodId: String = ""
    private var foodName: String = ""
    private var sellerId: String = ""

    // Current selected rating (1-5)
    private var currentRating: Int = 0

    // Cache list of star ImageViews
    private lateinit var stars: List<ImageView>

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_FOOD_ID = "extra_food_id"
        const val EXTRA_FOOD_NAME = "extra_food_name"
        const val EXTRA_SELLER_ID = "extra_seller_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        foodId = intent.getStringExtra(EXTRA_FOOD_ID) ?: ""
        foodName = intent.getStringExtra(EXTRA_FOOD_NAME) ?: ""
        sellerId = intent.getStringExtra(EXTRA_SELLER_ID) ?: ""

        if (orderId.isEmpty() || foodId.isEmpty()) {
            Toast.makeText(this, "Invalid review data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup stars list
        stars = listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5
        )

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.tvFoodName.text = foodName
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { submitReview() }

        // Setup star click listeners
        for ((index, star) in stars.withIndex()) {
            star.setOnClickListener {
                val rating = index + 1
                setRating(rating)
            }
        }
    }

    /**
     * Set rating dan update tampilan stars.
     */
    private fun setRating(rating: Int) {
        currentRating = rating

        // Update stars visual
        for ((index, star) in stars.withIndex()) {
            val drawable = if (index < rating) {
                R.drawable.ic_star_filled
            } else {
                R.drawable.ic_star_outline
            }
            star.setImageResource(drawable)
        }

        // Update label
        binding.tvRatingLabel.visibility = View.VISIBLE
        binding.tvRatingLabel.text = when (rating) {
            1 -> getString(R.string.rating_1_star)
            2 -> getString(R.string.rating_2_stars)
            3 -> getString(R.string.rating_3_stars)
            4 -> getString(R.string.rating_4_stars)
            5 -> getString(R.string.rating_5_stars)
            else -> ""
        }
    }

    /**
     * Validate form inputs.
     */
    private fun validateInputs(): Boolean {
        if (currentRating == 0) {
            Toast.makeText(this,
                getString(R.string.review_error_rating_required),
                Toast.LENGTH_SHORT).show()
            return false
        }

        val comment = binding.etComment.text.toString().trim()
        if (comment.length < 5) {
            binding.etComment.error = getString(R.string.review_error_comment_short)
            binding.etComment.requestFocus()
            return false
        }

        return true
    }

    private fun submitReview() {
        if (!validateInputs()) return

        val comment = binding.etComment.text.toString().trim()

        showLoading(true)

        // Get current user name for buyerName field
        val userId = FirebaseHelper.getCurrentUserId()
        if (userId == null) {
            showLoading(false)
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseHelper.firestore
            .collection(FirebaseHelper.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val buyerName = doc.getString("name") ?: "Anonymous"

                // Submit review
                ReviewRepository.submitReview(
                    orderId = orderId,
                    foodId = foodId,
                    sellerId = sellerId,
                    buyerName = buyerName,
                    rating = currentRating,
                    comment = comment,
                    onSuccess = {
                        Toast.makeText(this,
                            getString(R.string.write_review_success),
                            Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailure = { errorMessage ->
                        showLoading(false)
                        Toast.makeText(this,
                            getString(R.string.write_review_failed) + ": $errorMessage",
                            Toast.LENGTH_LONG).show()
                    }
                )
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !show
        binding.btnBack.isEnabled = !show
        binding.btnSubmit.text = if (show) {
            getString(R.string.write_review_submitting)
        } else {
            getString(R.string.write_review_submit)
        }

        // Disable star clicks saat loading
        for (star in stars) {
            star.isEnabled = !show
        }
    }
}