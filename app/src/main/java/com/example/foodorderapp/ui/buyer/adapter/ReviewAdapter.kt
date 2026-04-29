package com.example.foodorderapp.ui.buyer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Review
import com.example.foodorderapp.databinding.ItemReviewBinding
import java.util.concurrent.TimeUnit

class ReviewAdapter(
    private var reviews: List<Review>
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            // Reviewer name & initial
            val name = if (review.buyerName.isNotEmpty()) {
                review.buyerName
            } else {
                "Anonymous"
            }
            binding.tvReviewerName.text = name
            binding.tvReviewerInitial.text = name.firstOrNull()?.uppercase() ?: "A"

            // Date
            binding.tvReviewDate.text = getTimeAgo(review.createdAt)

            // Stars
            displayStars(review.rating)

            // Comment
            binding.tvReviewComment.text = review.comment
        }

        /**
         * Tampilkan stars filled sesuai rating.
         */
        private fun displayStars(rating: Int) {
            val stars = listOf(
                binding.star1,
                binding.star2,
                binding.star3,
                binding.star4,
                binding.star5
            )

            for ((index, star) in stars.withIndex()) {
                val drawable = if (index < rating) {
                    R.drawable.ic_star_filled
                } else {
                    R.drawable.ic_star_outline
                }
                star.setImageResource(drawable)
            }
        }

        /**
         * Format relative time.
         */
        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$mins min${if (mins > 1) "s" else ""} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours hour${if (hours > 1) "s" else ""} ago"
                }
                diff < TimeUnit.DAYS.toMillis(30) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days day${if (days > 1) "s" else ""} ago"
                }
                else -> {
                    val months = TimeUnit.MILLISECONDS.toDays(diff) / 30
                    "$months month${if (months > 1) "s" else ""} ago"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    fun updateReviews(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
}