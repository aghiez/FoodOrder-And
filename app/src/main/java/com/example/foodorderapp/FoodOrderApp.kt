package com.example.foodorderapp

import android.app.Application
import com.cloudinary.android.MediaManager

class FoodOrderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initCloudinary()
    }

    private fun initCloudinary() {
        try {
            val config = HashMap<String, String>()
            // Hanya butuh cloud_name untuk unsigned upload
            config["cloud_name"] = "didszf0pr"  // ← GANTI INI

            MediaManager.init(this, config)
        } catch (e: IllegalStateException) {
            // MediaManager sudah di-init sebelumnya, abaikan
            e.printStackTrace()
        }
    }
}