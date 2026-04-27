package com.example.foodorderapp.data.remote

import android.content.Context
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File
import java.io.FileOutputStream

object CloudinaryHelper {

    // GANTI INI dengan preset name dari Cloudinary Dashboard
    private const val UPLOAD_PRESET = "foodorderapp_preset"

    // Folder di dalam upload preset
    const val FOLDER_MENU_IMAGES = "menu_images"
    const val FOLDER_PROFILE_IMAGES = "profile_images"

    /**
     * Upload byte array (dari ImageUtils.processImageForUpload) ke Cloudinary.
     *
     * @param context Application context (untuk simpan file temporary)
     * @param imageBytes Hasil dari ImageUtils.processImageForUpload()
     * @param folder Folder name di Cloudinary (gunakan constants di atas)
     * @param publicId Optional - kalau dikasih, file akan di-overwrite kalau ada
     * @param onProgress Progress callback (0-100)
     * @param onSuccess Callback dengan URL & publicId
     * @param onFailure Callback dengan error message
     */
    fun uploadImage(
        context: Context,
        imageBytes: ByteArray,
        folder: String,
        publicId: String? = null,
        onProgress: ((progress: Int) -> Unit)? = null,
        onSuccess: (imageUrl: String, publicId: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        try {
            // Cloudinary SDK butuh File path, jadi kita save bytes ke temp file dulu
            val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { it.write(imageBytes) }

            val request = MediaManager.get().upload(tempFile.absolutePath)
                .unsigned(UPLOAD_PRESET)
                .option("folder", folder)

            // Optional: kasih publicId tertentu (untuk overwrite saat edit)
            if (publicId != null) {
                request.option("public_id", publicId)
                request.option("overwrite", true)
            }

            request.callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Upload dimulai
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = ((bytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                    onProgress?.invoke(progress)
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // Cleanup temp file
                    tempFile.delete()

                    val secureUrl = resultData["secure_url"] as? String ?: ""
                    val resultPublicId = resultData["public_id"] as? String ?: ""

                    if (secureUrl.isNotEmpty()) {
                        onSuccess(secureUrl, resultPublicId)
                    } else {
                        onFailure("Upload succeeded but URL is empty")
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    tempFile.delete()
                    onFailure(error.description ?: "Upload failed")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Upload akan di-retry oleh SDK, tidak perlu action
                }
            }).dispatch()

        } catch (e: Exception) {
            onFailure(e.message ?: "Unknown error")
        }
    }

    /**
     * Generate URL dengan transformation (resize on-the-fly).
     * Cloudinary otomatis cache di CDN.
     *
     * Contoh: getThumbnailUrl(originalUrl, 200)
     * → https://res.cloudinary.com/xxx/image/upload/w_200,h_200,c_fill,q_auto,f_auto/v123/.../abc.jpg
     */
    fun getThumbnailUrl(originalUrl: String, size: Int = 200): String {
        if (originalUrl.isEmpty() || !originalUrl.contains("/upload/")) {
            return originalUrl
        }

        val transformation = "w_$size,h_$size,c_fill,q_auto,f_auto"
        return originalUrl.replace("/upload/", "/upload/$transformation/")
    }

    /**
     * Generate URL untuk full size dengan auto quality & format.
     */
    fun getOptimizedUrl(originalUrl: String): String {
        if (originalUrl.isEmpty() || !originalUrl.contains("/upload/")) {
            return originalUrl
        }

        return originalUrl.replace("/upload/", "/upload/q_auto,f_auto/")
    }
}