package com.example.data.model

import java.security.MessageDigest

sealed interface PasswordChangeValidation {
    data class Valid(val newHash: String) : PasswordChangeValidation
    data class Invalid(val errorMessage: String) : PasswordChangeValidation
}

object AdminSecurityPolicy {
    const val DEFAULT_PASSCODE = "admin123"
    const val MIN_PASSCODE_LENGTH = 4
    private const val HASH_SALT = "cinetra_admin_v1:"

    fun hashPasscode(passcode: String): String {
        val normalized = passcode.trim()
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$HASH_SALT$normalized".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPasscode(input: String, storedHash: String): Boolean {
        val normalized = input.trim()
        if (normalized.isEmpty()) return false
        return hashPasscode(normalized) == storedHash
    }

    fun validatePasswordChange(
        currentInput: String,
        newPassword: String,
        confirmPassword: String,
        storedHash: String
    ): PasswordChangeValidation {
        if (currentInput.trim().isEmpty()) {
            return PasswordChangeValidation.Invalid("Please enter your current password.")
        }
        if (!verifyPasscode(currentInput, storedHash)) {
            return PasswordChangeValidation.Invalid("Current password is incorrect.")
        }
        val cleanNew = newPassword.trim()
        if (cleanNew.length < MIN_PASSCODE_LENGTH) {
            return PasswordChangeValidation.Invalid(
                "New password must be at least $MIN_PASSCODE_LENGTH characters."
            )
        }
        if (cleanNew == currentInput.trim()) {
            return PasswordChangeValidation.Invalid(
                "New password must be different from your current password."
            )
        }
        if (cleanNew != confirmPassword.trim()) {
            return PasswordChangeValidation.Invalid(
                "New password and confirmation do not match."
            )
        }
        return PasswordChangeValidation.Valid(hashPasscode(cleanNew))
    }
}
