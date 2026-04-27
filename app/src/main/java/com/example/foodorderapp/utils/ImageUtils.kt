package com.example.foodorderapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.IOException

object ImageUtils {

    private const val MAX_IMAGE_DIMENSION = 1024
    private const val COMPRESSION_QUALITY = 80

    /**
     * Load Bitmap from URI dengan handling untuk berbagai versi Android.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(
                    context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(
                    context.contentResolver, uri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resize bitmap agar tidak terlalu besar (max 1024px).
     * Penting untuk hemat upload bandwidth.
     */
    fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = MAX_IMAGE_DIMENSION
            newHeight = (MAX_IMAGE_DIMENSION / ratio).toInt()
        } else {
            newHeight = MAX_IMAGE_DIMENSION
            newWidth = (MAX_IMAGE_DIMENSION * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Compress Bitmap to JPEG byte array.
     * Quality 80 = balance antara size dan quality.
     */
    fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * All-in-one: URI → resized bitmap → compressed bytes.
     */
    fun processImageForUpload(context: Context, uri: Uri): ByteArray? {
        val bitmap = loadBitmapFromUri(context, uri) ?: return null
        val resizedBitmap = resizeBitmap(bitmap)
        return bitmapToBytes(resizedBitmap)
    }

    /**
     * Get permission name based on Android version.
     */
    fun getReadImagesPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}