package com.example.foodorderapp.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText

/**
 * Helper untuk real-time form validation.
 *
 * Untuk Materi Ajar:
 * - TextWatcher = listener yang fire saat text berubah
 * - 3 method: beforeTextChanged, onTextChanged, afterTextChanged
 * - Best practice: validate di afterTextChanged
 */
object ValidationHelper {

    /**
     * Result validasi.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    // ============================================
    // EMAIL VALIDATION
    // ============================================

    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isEmpty() ->
                ValidationResult(false, "Email is required")

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                ValidationResult(false, "Invalid email format")

            else -> ValidationResult(true)
        }
    }

    /**
     * Attach real-time email validator ke EditText.
     */
    fun attachEmailValidator(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s?.toString()?.trim() ?: ""
                if (email.isEmpty()) {
                    editText.error = null
                    return
                }

                val result = validateEmail(email)
                editText.error = result.errorMessage
            }
        })
    }

    // ============================================
    // PASSWORD VALIDATION
    // ============================================

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() ->
                ValidationResult(false, "Password is required")

            password.length < 6 ->
                ValidationResult(false, "Password must be at least 6 characters")

            else -> ValidationResult(true)
        }
    }

    /**
     * Attach real-time password validator.
     */
    fun attachPasswordValidator(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString() ?: ""
                if (password.isEmpty()) {
                    editText.error = null
                    return
                }

                val result = validatePassword(password)
                editText.error = result.errorMessage
            }
        })
    }

    // ============================================
    // PASSWORD STRENGTH (Bonus)
    // ============================================

    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG
    }

    fun getPasswordStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK

        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score >= 3 -> PasswordStrength.STRONG
            score >= 1 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    // ============================================
    // CONFIRM PASSWORD VALIDATION
    // ============================================

    /**
     * Attach validator: confirm password harus match dengan password.
     */
    fun attachConfirmPasswordValidator(
        passwordField: EditText,
        confirmPasswordField: EditText
    ) {
        confirmPasswordField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val confirmPassword = s?.toString() ?: ""
                val password = passwordField.text.toString()

                if (confirmPassword.isEmpty()) {
                    confirmPasswordField.error = null
                    return
                }

                if (confirmPassword != password) {
                    confirmPasswordField.error = "Passwords don't match"
                } else {
                    confirmPasswordField.error = null
                }
            }
        })
    }

    // ============================================
    // PHONE VALIDATION
    // ============================================

    fun validatePhone(phone: String): ValidationResult {
        return when {
            phone.isEmpty() ->
                ValidationResult(false, "Phone is required")

            !phone.matches(Regex("^(\\+62|0)\\d{9,13}$")) ->
                ValidationResult(false, "Invalid phone format (use 08xxx)")

            else -> ValidationResult(true)
        }
    }

    fun attachPhoneValidator(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val phone = s?.toString()?.trim() ?: ""
                if (phone.isEmpty()) {
                    editText.error = null
                    return
                }

                val result = validatePhone(phone)
                editText.error = result.errorMessage
            }
        })
    }

    // ============================================
    // NAME VALIDATION
    // ============================================

    fun validateName(name: String, minLength: Int = 3): ValidationResult {
        return when {
            name.isEmpty() ->
                ValidationResult(false, "Name is required")

            name.length < minLength ->
                ValidationResult(false, "Name too short (min $minLength chars)")

            else -> ValidationResult(true)
        }
    }

    fun attachNameValidator(editText: EditText, minLength: Int = 3) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = s?.toString()?.trim() ?: ""
                if (name.isEmpty()) {
                    editText.error = null
                    return
                }

                val result = validateName(name, minLength)
                editText.error = result.errorMessage
            }
        })
    }

    // ============================================
    // PRICE / NUMBER VALIDATION
    // ============================================

    fun validatePositiveNumber(value: String, fieldName: String = "Value"): ValidationResult {
        return when {
            value.isEmpty() ->
                ValidationResult(false, "$fieldName is required")

            value.toDoubleOrNull() == null ->
                ValidationResult(false, "Invalid number")

            value.toDouble() <= 0 ->
                ValidationResult(false, "$fieldName must be greater than 0")

            else -> ValidationResult(true)
        }
    }
}