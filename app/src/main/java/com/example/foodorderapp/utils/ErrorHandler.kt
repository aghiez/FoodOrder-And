package com.example.foodorderapp.utils

import android.content.Context
import com.example.foodorderapp.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Convert technical error ke user-friendly message.
 *
 * Untuk Materi Ajar:
 * - Pattern: when expression untuk type matching
 * - String resources untuk i18n
 * - Centralized error handling = consistent UX
 */
object ErrorHandler {

    /**
     * Convert Throwable ke user-friendly message.
     */
    fun getFriendlyMessage(context: Context, throwable: Throwable?): String {
        if (throwable == null) {
            return context.getString(R.string.error_unknown)
        }

        return when (throwable) {

            // Network errors
            is FirebaseNetworkException ->
                context.getString(R.string.error_no_internet)

            // Firestore errors
            is FirebaseFirestoreException ->
                handleFirestoreException(context, throwable)

            // Firebase Auth errors
            is FirebaseAuthInvalidUserException ->
                context.getString(R.string.error_auth_invalid)

            is FirebaseAuthInvalidCredentialsException ->
                context.getString(R.string.error_auth_invalid)

            is FirebaseAuthUserCollisionException ->
                context.getString(R.string.error_auth_email_used)

            is FirebaseAuthWeakPasswordException ->
                context.getString(R.string.error_auth_weak_password)

            is FirebaseAuthException ->
                handleAuthException(context, throwable)

            // Generic exceptions
            else -> {
                val message = throwable.message ?: ""
                when {
                    message.contains("network", ignoreCase = true) ->
                        context.getString(R.string.error_no_internet)

                    message.contains("timeout", ignoreCase = true) ->
                        context.getString(R.string.error_timeout)

                    message.contains("permission", ignoreCase = true) ->
                        context.getString(R.string.error_permission_denied)

                    else -> context.getString(R.string.error_unknown)
                }
            }
        }
    }

    /**
     * Handle Firestore-specific exceptions.
     */
    private fun handleFirestoreException(
        context: Context,
        e: FirebaseFirestoreException
    ): String {
        return when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                context.getString(R.string.error_permission_denied)

            FirebaseFirestoreException.Code.NOT_FOUND ->
                context.getString(R.string.error_not_found)

            FirebaseFirestoreException.Code.UNAVAILABLE ->
                context.getString(R.string.error_no_internet)

            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                context.getString(R.string.error_timeout)

            FirebaseFirestoreException.Code.INVALID_ARGUMENT ->
                context.getString(R.string.error_invalid_data)

            else -> context.getString(R.string.error_server)
        }
    }

    /**
     * Handle Firebase Auth exceptions berdasarkan error code.
     */
    private fun handleAuthException(
        context: Context,
        e: FirebaseAuthException
    ): String {
        return when (e.errorCode) {
            "ERROR_USER_DISABLED" ->
                context.getString(R.string.error_auth_user_disabled)

            "ERROR_TOO_MANY_REQUESTS" ->
                context.getString(R.string.error_auth_too_many)

            else -> context.getString(R.string.error_auth_invalid)
        }
    }

    /**
     * Convert string error message (dari callback onFailure) ke friendly.
     * Useful saat repository return error sebagai String.
     */
    fun getFriendlyMessage(context: Context, errorString: String?): String {
        if (errorString.isNullOrEmpty()) {
            return context.getString(R.string.error_unknown)
        }

        val lower = errorString.lowercase()

        return when {
            lower.contains("permission_denied") ||
                    lower.contains("permission denied") ->
                context.getString(R.string.error_permission_denied)

            lower.contains("network") ||
                    lower.contains("unavailable") ->
                context.getString(R.string.error_no_internet)

            lower.contains("timeout") ||
                    lower.contains("deadline") ->
                context.getString(R.string.error_timeout)

            lower.contains("not found") ->
                context.getString(R.string.error_not_found)

            lower.contains("invalid") ->
                context.getString(R.string.error_invalid_data)

            else -> context.getString(R.string.error_unknown)
        }
    }
}