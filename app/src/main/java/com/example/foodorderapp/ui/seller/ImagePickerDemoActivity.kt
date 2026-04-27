package com.example.foodorderapp.ui.seller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.foodorderapp.R
import com.example.foodorderapp.data.remote.CloudinaryHelper
import com.example.foodorderapp.databinding.ActivityImagePickerDemoBinding
import com.example.foodorderapp.utils.ImageUtils

class ImagePickerDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePickerDemoBinding
    private var selectedImageUri: Uri? = null

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
        binding = ActivityImagePickerDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPick.setOnClickListener { onPickImageClick() }
        binding.imageContainer.setOnClickListener { onPickImageClick() }
        binding.btnTestUpload.setOnClickListener { testUpload() }
    }

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
            .into(binding.ivSelected)

        binding.ivSelected.visibility = View.VISIBLE
        binding.llPlaceholder.visibility = View.GONE
        binding.tvImageInfo.visibility = View.VISIBLE
        binding.btnPick.text = getString(R.string.img_pick_change)
        binding.btnTestUpload.isEnabled = true

        displayImageInfo(uri)
    }

    private fun displayImageInfo(uri: Uri) {
        try {
            val bitmap = ImageUtils.loadBitmapFromUri(this, uri)
            if (bitmap != null) {
                val originalSize = bitmap.byteCount / 1024
                val resized = ImageUtils.resizeBitmap(bitmap)
                val compressedBytes = ImageUtils.bitmapToBytes(resized)
                val compressedSize = compressedBytes.size / 1024

                binding.tvImageInfo.text = """
                    Original: ${bitmap.width}x${bitmap.height} (${originalSize} KB)
                    Compressed: ${resized.width}x${resized.height} (${compressedSize} KB)
                """.trimIndent()
            }
        } catch (e: Exception) {
            binding.tvImageInfo.text = "Error: ${e.message}"
        }
    }

    /**
     * Test upload ke Cloudinary.
     */
    private fun testUpload() {
        val uri = selectedImageUri ?: run {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Process image (resize + compress)
        val imageBytes = ImageUtils.processImageForUpload(this, uri)
        if (imageBytes == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
            return
        }

        // Reset UI
        binding.btnTestUpload.isEnabled = false
        binding.btnTestUpload.text = "Uploading..."
        binding.progressUpload.visibility = View.VISIBLE
        binding.progressUpload.progress = 0
        binding.tvUploadResult.visibility = View.VISIBLE
        binding.tvUploadResult.text = "Uploading to Cloudinary..."

        CloudinaryHelper.uploadImage(
            context = applicationContext,
            imageBytes = imageBytes,
            folder = CloudinaryHelper.FOLDER_MENU_IMAGES,
            onProgress = { progress ->
                runOnUiThread {
                    binding.progressUpload.progress = progress
                    binding.tvUploadResult.text = "Uploading: $progress%"
                }
            },
            onSuccess = { imageUrl, publicId ->
                runOnUiThread {
                    binding.btnTestUpload.text = "Upload Test Again"
                    binding.btnTestUpload.isEnabled = true
                    binding.tvUploadResult.text = """
                        ✅ Upload SUCCESS!
                        URL: $imageUrl
                        Public ID: $publicId
                    """.trimIndent()

                    Toast.makeText(this, "Upload success! Check Cloudinary console.",
                        Toast.LENGTH_LONG).show()
                }
            },
            onFailure = { errorMessage ->
                runOnUiThread {
                    binding.btnTestUpload.text = "Test Upload to Cloudinary"
                    binding.btnTestUpload.isEnabled = true
                    binding.progressUpload.visibility = View.GONE
                    binding.tvUploadResult.text = "❌ Error: $errorMessage"

                    Toast.makeText(this, "Upload failed: $errorMessage",
                        Toast.LENGTH_LONG).show()
                }
            }
        )
    }

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