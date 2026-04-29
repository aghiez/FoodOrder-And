package com.example.foodorderapp.ui.seller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.foodorderapp.R
import com.example.foodorderapp.data.model.Category
import com.example.foodorderapp.data.model.Food
import com.example.foodorderapp.data.repository.MenuRepository
import com.example.foodorderapp.databinding.ActivityAddEditMenuBinding
import com.example.foodorderapp.utils.ImageUtils
import com.example.foodorderapp.data.remote.CloudinaryHelper
import com.example.foodorderapp.utils.ErrorHandler
import com.example.foodorderapp.utils.NetworkUtil
import com.example.foodorderapp.utils.SnackbarHelper

class AddEditMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditMenuBinding

    // Mode: Add (foodId null) atau Edit (foodId ada)
    private var foodId: String? = null
    private var isEditMode: Boolean = false

    // Categories untuk Spinner
    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: String = ""

    // Selected image
    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""  // untuk mode edit

    companion object {
        const val EXTRA_FOOD_ID = "extra_food_id"
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchImagePicker()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleImageSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Detect mode dari extra
        foodId = intent.getStringExtra(EXTRA_FOOD_ID)
        isEditMode = !foodId.isNullOrEmpty()

        setupUI()
        setupListeners()
        loadCategories()

        if (isEditMode) {
            loadFoodForEdit()
        }
    }

    private fun setupUI() {
        // Set title & button text based on mode
        if (isEditMode) {
            binding.tvTitle.text = getString(R.string.edit_menu_title)
            binding.btnSave.text = getString(R.string.menu_form_update)
        } else {
            binding.tvTitle.text = getString(R.string.add_menu_title)
            binding.btnSave.text = getString(R.string.menu_form_save)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            if (hasUnsavedChanges()) {
                showUnsavedChangesDialog()
            } else {
                finish()
            }
        }

        binding.imageContainer.setOnClickListener { onPickImageClick() }
        binding.btnSave.setOnClickListener { saveMenu() }

        // Spinner listener
        binding.spinnerCategory.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                            position: Int, id: Long) {
                    if (position > 0 && position <= categories.size) {
                        selectedCategoryId = categories[position - 1].id
                    } else {
                        selectedCategoryId = ""
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedCategoryId = ""
                }
            }
    }

    private fun loadCategories() {
        MenuRepository.getCategories(
            onSuccess = { categoryList ->
                categories = categoryList
                setupCategorySpinner()
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, "Failed to load categories: $errorMessage",
                    Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupCategorySpinner() {
        // Tambah "Select Category" sebagai item pertama
        val categoryNames = mutableListOf("-- Select Category --")
        categoryNames.addAll(categories.map { it.name })

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Kalau mode edit dan ada existing categoryId, set selection
        if (isEditMode && selectedCategoryId.isNotEmpty()) {
            val index = categories.indexOfFirst { it.id == selectedCategoryId }
            if (index >= 0) {
                binding.spinnerCategory.setSelection(index + 1)
            }
        }
    }

    private fun loadFoodForEdit() {
        val id = foodId ?: return

        MenuRepository.getMenuById(
            foodId = id,
            onSuccess = { food ->
                fillFormWithFoodData(food)
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun fillFormWithFoodData(food: Food) {
        binding.etName.setText(food.name)
        binding.etDescription.setText(food.description)
        binding.etPrice.setText(food.price.toLong().toString())
        binding.etStock.setText(food.stock.toString())

        selectedCategoryId = food.categoryId
        existingImageUrl = food.imageUrl

        // Display existing image
        if (food.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(food.imageUrl)
                .placeholder(R.drawable.ic_food)
                .into(binding.ivMenuImage)
            binding.ivMenuImage.visibility = View.VISIBLE
            binding.llImagePlaceholder.visibility = View.GONE
        }

        // Setup spinner kalau categories sudah loaded
        if (categories.isNotEmpty()) {
            val index = categories.indexOfFirst { it.id == food.categoryId }
            if (index >= 0) {
                binding.spinnerCategory.setSelection(index + 1)
            }
        }
    }

    // ========== IMAGE PICKER ==========

    private fun onPickImageClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchImagePicker()
            return
        }

        val permission = ImageUtils.getReadImagesPermission()
        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> launchImagePicker()
            shouldShowRequestPermissionRationale(permission) ->
                showPermissionRationale(permission)
            else -> permissionLauncher.launch(permission)
        }
    }

    private fun launchImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun handleImageSelected(uri: Uri) {
        selectedImageUri = uri

        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.ivMenuImage)

        binding.ivMenuImage.visibility = View.VISIBLE
        binding.llImagePlaceholder.visibility = View.GONE
    }

    // ========== VALIDATION & SAVE ==========

    /**
     * Validate semua input. Return true kalau valid.
     */
    private fun validateInputs(): Boolean {
        // Image: required untuk Add, optional untuk Edit (kalau sudah ada)
        if (!isEditMode && selectedImageUri == null) {
            Toast.makeText(this, getString(R.string.error_image_required),
                Toast.LENGTH_SHORT).show()
            return false
        }

        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_menu_name_required)
            binding.etName.requestFocus()
            return false
        }
        if (name.length < 3) {
            binding.etName.error = getString(R.string.error_menu_name_short)
            binding.etName.requestFocus()
            return false
        }

        val description = binding.etDescription.text.toString().trim()
        if (description.isEmpty()) {
            binding.etDescription.error = getString(R.string.error_description_required)
            binding.etDescription.requestFocus()
            return false
        }

        if (selectedCategoryId.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_category_required),
                Toast.LENGTH_SHORT).show()
            return false
        }

        val priceText = binding.etPrice.text.toString().trim()
        if (priceText.isEmpty()) {
            binding.etPrice.error = getString(R.string.error_price_required)
            binding.etPrice.requestFocus()
            return false
        }
        val price = priceText.toDoubleOrNull()
        if (price == null) {
            binding.etPrice.error = getString(R.string.error_price_invalid)
            binding.etPrice.requestFocus()
            return false
        }
        if (price < 1000) {
            binding.etPrice.error = getString(R.string.error_price_min)
            binding.etPrice.requestFocus()
            return false
        }

        val stockText = binding.etStock.text.toString().trim()
        if (stockText.isEmpty()) {
            binding.etStock.error = getString(R.string.error_stock_required)
            binding.etStock.requestFocus()
            return false
        }
        val stock = stockText.toIntOrNull()
        if (stock == null || stock < 0) {
            binding.etStock.error = getString(R.string.error_stock_invalid)
            binding.etStock.requestFocus()
            return false
        }

        return true
    }

    /**
     * Save menu. Untuk C.4.3 ini, tanpa upload dulu.
     * Image upload akan ditambah di C.4.4.
     */
    private fun saveMenu() {
        if (!validateInputs()) return

        if(!NetworkUtil.isOnline(this)){
            SnackbarHelper.showNoInternet(binding.root) {
                saveMenu()
            }
            return
        }

        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val price = binding.etPrice.text.toString().trim().toDouble()
        val stock = binding.etStock.text.toString().trim().toInt()

        showLoading(true, "Processing...")

        if(selectedImageUri != null){
            uploadNewImageThenUpdate(name, description, price, stock)
        } else {
            if (isEditMode) {
                updateFirestoreOnly(name, description, price, stock, null)
            } else {
                saveToFirestore(existingImageUrl)
            }
        }
    }

    /**
     * Add mode: Upload image ke Cloudinary → Save ke Firestore.
     */
    private fun handleAddSave() {
        val imageUri = selectedImageUri ?: run {
            Toast.makeText(this, getString(R.string.error_image_required),
                Toast.LENGTH_SHORT).show()
            return
        }

        // Step 1: Process image (resize + compress)
        showLoading(true, getString(R.string.menu_form_uploading))

        val imageBytes = ImageUtils.processImageForUpload(this, imageUri)
        if (imageBytes == null) {
            showLoading(false, null)
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
            return
        }

        // Step 2: Upload ke Cloudinary
        CloudinaryHelper.uploadImage(
            context = applicationContext,
            imageBytes = imageBytes,
            folder = CloudinaryHelper.FOLDER_MENU_IMAGES,
            onProgress = { progress ->
                runOnUiThread {
                    if (!isFinishing) {
                        binding.progressBar.progress = progress
                        binding.tvProgressLabel.text = "Uploading image: $progress%"
                    }
                }
            },
            onSuccess = { imageUrl, _ ->
                runOnUiThread {
                    if (!isFinishing) {
                        // Step 3: Save ke Firestore dengan URL asli
                        saveToFirestore(imageUrl)
                    }
                }
            },
            onFailure = { errorMessage ->
                runOnUiThread {
                    if (!isFinishing) {
                        showLoading(false, null)
                        Toast.makeText(this, "Upload failed: $errorMessage",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    /**
     * Step 3: Save ke Firestore (dipanggil setelah Cloudinary upload sukses).
     */
    private fun saveToFirestore(imageUrl: String) {
        binding.tvProgressLabel.text = getString(R.string.menu_form_saving)
        binding.progressBar.progress = 100

        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val price = binding.etPrice.text.toString().trim().toDouble()
        val stock = binding.etStock.text.toString().trim().toInt()

        val newFood = Food(
            sellerId = "",  // akan di-set di repository
            categoryId = selectedCategoryId,
            name = name,
            description = description,
            price = price,
            imageUrl = imageUrl,  // URL dari Cloudinary
            isAvailable = true,
            stock = stock,
            rating = 0.0,
            totalSold = 0,
            createdAt = 0L,
            updatedAt = 0L
        )

        MenuRepository.addMenu(
            food = newFood,
            onSuccess = { foodId ->
                showLoading(false, null)
                Toast.makeText(this, getString(R.string.menu_form_save_success),
                    Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { errorMessage ->
                showLoading(false, null)
                Toast.makeText(this, "Save failed: $errorMessage",
                    Toast.LENGTH_LONG).show()
            }
        )

        val onSuccess: () -> Unit = {
            SnackbarHelper.showSuccess(
                view = binding.root,
                message = if(isEditMode) "Menu Updated!" else "Menu Added!"
            )
            binding.root.postDelayed({
                finish()
            },800)
        }
        val onFailure: (String) -> Unit = { errorMessage ->
            showLoading(false, null)

            val friendlyMeessage = ErrorHandler.getFriendlyMessage(this, errorMessage)
            SnackbarHelper.showErrorWithRetry(
                view = binding.root,
                message = friendlyMeessage
            ) {
                saveMenu() }
        }

        if(isEditMode){
            val newImageUrl = null
            MenuRepository.updateMenu(
                foodId = foodId!!,
                name = name,
                description = description,
                categoryId = selectedCategoryId,
                price = price,
                stock = stock,
                imageUrl = newImageUrl ?: existingImageUrl,
                onSuccess = {
                    //sukses
                },
                onFailure = {
                    //gagal
                }
            )
        }else{
            MenuRepository.addMenu(
                food = newFood,
                onSuccess = {
                    //sukses
                },
                onFailure = {
                    //gagal
                }
            )
        }
    }

    /**
     * Edit mode: kalau image diganti → upload dulu, kalau tidak → langsung update.
     */
    private fun handleEditSave() {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val price = binding.etPrice.text.toString().trim().toDouble()
        val stock = binding.etStock.text.toString().trim().toInt()

        // Cek: ada image baru yang dipilih?
        if (selectedImageUri != null) {
            // Image baru → upload dulu
            uploadNewImageThenUpdate(name, description, price, stock)
        } else {
            // Tidak ada image baru → langsung update tanpa ganti URL
            updateFirestoreOnly(name, description, price, stock, null)
        }
    }

    /**
     * Upload image baru ke Cloudinary, lalu update Firestore.
     */
    private fun uploadNewImageThenUpdate(name: String, description: String,
                                         price: Double, stock: Int) {
        val imageUri = selectedImageUri ?: return

        showLoading(true, getString(R.string.menu_form_uploading))

        val imageBytes = ImageUtils.processImageForUpload(this, imageUri)
        if (imageBytes == null) {
            showLoading(false, null)
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
            return
        }

        CloudinaryHelper.uploadImage(
            context = applicationContext,
            imageBytes = imageBytes,
            folder = CloudinaryHelper.FOLDER_MENU_IMAGES,
            onProgress = { progress ->
                runOnUiThread {
                    if (!isFinishing) {
                        binding.progressBar.progress = progress
                        binding.tvProgressLabel.text = "Uploading image: $progress%"
                    }
                }
            },
            onSuccess = { imageUrl, _ ->
                runOnUiThread {
                    if (!isFinishing) {
                        updateFirestoreOnly(name, description, price, stock, imageUrl)
                    }
                }
            },
            onFailure = { errorMessage ->
                runOnUiThread {
                    if (!isFinishing) {
                        showLoading(false, null)
                        Toast.makeText(this, "Upload failed: $errorMessage",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    /**
     * Update Firestore (dengan atau tanpa URL baru).
     */
    private fun updateFirestoreOnly(name: String, description: String,
                                    price: Double, stock: Int,
                                    newImageUrl: String?) {
        val id = foodId ?: return

        binding.tvProgressLabel.text = getString(R.string.menu_form_saving)

        MenuRepository.updateMenu(
            foodId = id,
            name = name,
            description = description,
            categoryId = selectedCategoryId,
            price = price,
            stock = stock,
            imageUrl = newImageUrl,  // null kalau tidak ganti foto
            onSuccess = {
                showLoading(false, null)
                Toast.makeText(this, getString(R.string.menu_form_update_success),
                    Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { errorMessage ->
                showLoading(false, null)
                Toast.makeText(this, "Update failed: $errorMessage",
                    Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showLoading(show: Boolean, label: String?) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvProgressLabel.visibility = if (show && label != null) View.VISIBLE else View.GONE
        binding.tvProgressLabel.text = label ?: ""
        binding.btnSave.isEnabled = !show
        binding.btnBack.isEnabled = !show  // disable back saat upload
        binding.imageContainer.isEnabled = !show  // disable change image saat upload

        binding.btnSave.text = when {
            show -> "Processing..."
            isEditMode -> getString(R.string.menu_form_update)
            else -> getString(R.string.menu_form_save)
        }

        // Reset progress saat tidak loading
        if (!show) {
            binding.progressBar.progress = 0
        }
    }

    // ========== UNSAVED CHANGES ==========

    private fun hasUnsavedChanges(): Boolean {
        if (isEditMode) {
            // Untuk simplicity, anggap selalu ada changes di edit mode
            return false  // bisa di-improve dengan compare value
        }

        return selectedImageUri != null ||
                binding.etName.text.toString().isNotBlank() ||
                binding.etDescription.text.toString().isNotBlank() ||
                binding.etPrice.text.toString().isNotBlank() ||
                binding.etStock.text.toString().isNotBlank()
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Changes?")
            .setMessage("Your changes will be lost.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    // ========== PERMISSION DIALOGS ==========

    private fun showPermissionRationale(permission: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_required_message)
            .setPositiveButton(R.string.permission_grant) { _, _ ->
                permissionLauncher.launch(permission)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_required_message)
            .setPositiveButton(R.string.permission_settings) { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}