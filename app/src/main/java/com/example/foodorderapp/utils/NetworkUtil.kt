package com.example.foodorderapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Utility untuk cek koneksi internet.
 *
 * Untuk Materi Ajar:
 * - Single Responsibility: kelas ini hanya urus network detection
 * - Modern API (NetworkCapabilities) untuk Android 6+
 * - Backwards compat untuk Android lama
 */
object NetworkUtil {

    /**
     * Cek apakah ada koneksi internet (WiFi atau Cellular).
     *
     * @return true kalau ada internet, false kalau offline
     */
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ : pakai modern API
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager
                .getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            // Android < 6 : pakai legacy API
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnectedOrConnecting == true
        }
    }

    /**
     * Cek tipe koneksi (WiFi, Cellular, atau None).
     * Berguna untuk warning "Sedang pakai data seluler".
     */
    fun getConnectionType(context: Context): ConnectionType {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return ConnectionType.NONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
                ?: return ConnectionType.NONE
            val capabilities = connectivityManager
                .getNetworkCapabilities(network)
                ?: return ConnectionType.NONE

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                    ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    ConnectionType.CELLULAR
                else -> ConnectionType.NONE
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> ConnectionType.WIFI
                ConnectivityManager.TYPE_MOBILE -> ConnectionType.CELLULAR
                else -> ConnectionType.NONE
            }
        }
    }

    enum class ConnectionType {
        WIFI, CELLULAR, NONE
    }
}