package com.example.foodorderapp.utils

import java.text.NumberFormat
import java.util.Locale

object Formatter {

    /**
     * Format harga ke Rupiah, contoh: 15000 → "Rp 15.000"
     */
    fun toRupiah(amount: Double): String {
        val locale = Locale("id", "ID")
        val format = NumberFormat.getNumberInstance(locale)
        return "Rp ${format.format(amount.toLong())}"
    }

    fun toRupiah(amount: Int): String = toRupiah(amount.toDouble())

    /**
     * Generate inisial dari nama.
     * Contoh: "Budi Santoso" → "BS", "Aghiez" → "A"
     */
    fun getInitials(name: String): String {
        if (name.isBlank()) return "?"

        val words = name.trim().split("\\s+".toRegex())
        return when {
            words.size >= 2 -> {
                "${words[0].first().uppercase()}${words[1].first().uppercase()}"
            }
            words.size == 1 -> {
                words[0].first().uppercase().toString()
            }
            else -> "?"
        }
    }

    /**
     * Format harga dengan singkatan untuk angka besar.
     * Contoh: 240000 → "Rp 240k", 1500000 → "Rp 1.5M"
     */
    fun toRupiahShort(amount: Double): String {
        return when {
            amount >= 1_000_000 -> {
                val millions = amount / 1_000_000
                "Rp ${"%.1f".format(millions)}M"
            }
            amount >= 1_000 -> {
                val thousands = amount / 1_000
                "Rp ${thousands.toInt()}k"
            }
            else -> "Rp ${amount.toInt()}"
        }
    }

    /**
     * Format date dari Long timestamp ke readable string.
     * Contoh: "Jan 2026"
     */
    fun toMonthYear(timestamp: Long): String {
        if (timestamp == 0L) return "-"
        val format = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale("id", "ID"))
        return format.format(java.util.Date(timestamp))
    }
}