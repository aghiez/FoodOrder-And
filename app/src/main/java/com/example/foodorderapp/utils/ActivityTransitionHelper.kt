package com.example.foodorderapp.utils

import android.app.Activity
import com.example.foodorderapp.R

/**
 * Helper untuk smooth Activity transitions.
 *
 * Untuk Materi Ajar:
 * - overridePendingTransition() control animasi antar Activity
 * - 2 parameter: enter animation + exit animation
 * - Pattern reusable untuk consistency
 */
object ActivityTransitionHelper {

    /**
     * Standard slide transition (kanan masuk, kiri keluar).
     * Pakai ini untuk navigasi normal forward.
     */
    fun slideForward(activity: Activity) {
        activity.overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    /**
     * Standard slide back transition (kiri masuk, kanan keluar).
     * Pakai ini saat finish activity (back navigation).
     */
    fun slideBack(activity: Activity) {
        activity.overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    /**
     * Slide up dari bottom.
     * Pakai untuk modal-like activities (detail page, form).
     */
    fun slideUp(activity: Activity) {
        activity.overridePendingTransition(
            R.anim.slide_in_bottom,
            android.R.anim.fade_out
        )
    }

    /**
     * Slide down ke bottom.
     * Pakai saat menutup modal.
     */
    fun slideDown(activity: Activity) {
        activity.overridePendingTransition(
            android.R.anim.fade_in,
            R.anim.slide_out_bottom
        )
    }

    /**
     * Simple fade transition.
     * Pakai untuk transisi tanpa direction (settings, dialog).
     */
    fun fade(activity: Activity) {
        activity.overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }
}