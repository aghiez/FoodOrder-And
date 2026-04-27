package com.example.foodorderapp.ui.buyer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.remote.FirebaseHelper
import com.example.foodorderapp.data.repository.CartRepository
import com.example.foodorderapp.databinding.ActivityFoodDetailBinding
import com.example.foodorderapp.utils.Formatter

class FoodDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoodDetailBinding
    private lateinit var food: Food
    private var sellerName: String = ""
    private var quantity: Int = 1

    companion object {
        const val EXTRA_FOOD = "extra_food"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get food data from Intent
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
    }

    private fun displayFoodData() {
        binding.tvFoodName.text = food.name
        binding.tvFoodDescription.text = food.description
        binding.tvFoodRating.text = food.rating.toString()
        binding.tvFoodSold.text = "• ${food.totalSold} sold"
        binding.tvFoodStock.text = "• " + getString(R.string.detail_stock, food.stock)

        Glide.with(this)
            .load(food.imageUrl)
            .placeholder(R.drawable.ic_food)
            .error(R.drawable.ic_food)
            .into(binding.ivFoodImage)
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

        binding.btnAddToCart.setOnClickListener {
            addToCart()
        }
    }

    private fun updateUI() {
        binding.tvQuantity.text = quantity.toString()

        // Update text tombol dengan total harga
        val total = food.price * quantity
        binding.btnAddToCart.text = "Add to Cart - ${Formatter.toRupiah(total)}"

        // Disable jika stok kosong
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
                finish()  // kembali ke HomeFragment
            },
            onFailure = { errorMessage ->
                binding.btnAddToCart.isEnabled = true
                updateUI()
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }
}