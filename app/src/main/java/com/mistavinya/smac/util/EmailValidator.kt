package com.mistavinya.smac.util

object EmailValidator {
    private val PERSONAL_DOMAINS = listOf(
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
        "rediffmail.com", "icloud.com", "protonmail.com", "mail.com"
    )

    fun isWorkEmail(email: String): Boolean {
        if (email.isBlank() || !email.contains("@")) return false
        val domain = email.substringAfterLast("@").lowercase()
        return domain !in PERSONAL_DOMAINS
    }

    fun getEmailError(email: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            !isWorkEmail(email) -> "Please use a work email address"
            else -> null
        }
    }
}
