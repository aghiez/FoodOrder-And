package com.example.foodorderapp.utils

import android.view.View
import androidx.core.content.ContextCompat
import com.example.foodorderapp.R
import com.google.android.material.snackbar.Snackbar

/**
 * Helper untuk show Snackbar dengan styling dan action konsisten.
 *
 * Untuk Materi Ajar:
 * - Snackbar = Toast yang lebih powerful
 * - Bisa punya action button (Retry, Undo, dll)
 * - Built-in animation, swipe-to-dismiss
 * - Material Design recommended
 */
object SnackbarHelper {

    /**
     * Show simple message (mirip Toast tapi lebih elegant).
     */
    fun show(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(view, message, duration).show()
    }

    /**
     * Show error message dengan styling merah dan action button.
     */
    fun showError(
        view: View,
        message: String,
        actionText: String? = null,
        onActionClick: (() -> Unit)? = null
    ) {
        val context = view.context
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)

        // Background merah untuk error
        snackbar.setBackgroundTint(
            ContextCompat.getColor(context, R.color.error))
        snackbar.setTextColor(
            ContextCompat.getColor(context, android.R.color.white))

        // Action button (kalau ada)
        if (actionText != null && onActionClick != null) {
            snackbar.setAction(actionText) { onActionClick() }
            snackbar.setActionTextColor(
                ContextCompat.getColor(context, android.R.color.white))
        }

        snackbar.show()
    }

    /**
     * Show success message dengan styling hijau.
     */
    fun showSuccess(view: View, message: String) {
        val context = view.context
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)

        snackbar.setBackgroundTint(
            ContextCompat.getColor(context, R.color.success))
        snackbar.setTextColor(
            ContextCompat.getColor(context, android.R.color.white))

        snackbar.show()
    }

    /**
     * Show info message dengan styling primary.
     */
    fun showInfo(view: View, message: String) {
        val context = view.context
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)

        snackbar.setBackgroundTint(
            ContextCompat.getColor(context, R.color.primary))
        snackbar.setTextColor(
            ContextCompat.getColor(context, android.R.color.white))

        snackbar.show()
    }

    /**
     * Show error dengan tombol Retry default.
     */
    fun showErrorWithRetry(
        view: View,
        message: String,
        onRetry: () -> Unit
    ) {
        val context = view.context
        showError(
            view = view,
            message = message,
            actionText = context.getString(R.string.action_retry),
            onActionClick = onRetry
        )
    }

    /**
     * Show no internet error dengan retry.
     */
    fun showNoInternet(view: View, onRetry: () -> Unit) {
        val context = view.context
        showErrorWithRetry(
            view = view,
            message = context.getString(R.string.error_no_internet),
            onRetry = onRetry
        )
    }
}