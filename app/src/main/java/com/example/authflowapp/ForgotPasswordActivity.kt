package com.example.authflowapp

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.authflowapp.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * ForgotPasswordActivity handles password reset with Firebase Authentication.
 *
 * Navigation Flow:
 * - Opened from LoginActivity
 * - After sending reset email → returns to LoginActivity
 * - "Back to Login" link → returns to LoginActivity
 *
 * Functionality:
 * - Validates email input (empty check + format validation)
 * - Sends password reset email via Firebase
 * - Shows success/error messages
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Send Reset Email button - validates and sends reset email
        binding.btnSendResetEmail.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (validateEmail(email)) {
                sendPasswordResetEmail(email)
            }
        }

        // Navigate back to LoginActivity
        binding.tvBackToLogin.setOnClickListener {
            finish() // Returns to LoginActivity
        }
    }

    /**
     * Validates the email input field.
     * Checks for:
     * 1. Empty field
     * 2. Valid email format using Android Patterns
     *
     * @param email The email address to validate
     * @return true if email is valid, false otherwise
     */
    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Enter a valid email address"
                false
            }
            else -> {
                binding.tilEmail.error = null
                true
            }
        }
    }

    /**
     * Sends password reset email using Firebase Authentication.
     * On success, shows success message and navigates back to LoginActivity.
     *
     * @param email The email address to send reset link to
     */
    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(
                    this,
                    "Password reset email sent. Please check your inbox.",
                    Toast.LENGTH_LONG
                ).show()
                // Navigate back to login after successful email send
                finish()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                handleFirebaseError(exception)
            }
    }

    /**
     * Handles Firebase errors with security-conscious messaging.
     * Does not reveal whether an email is registered to prevent enumeration attacks.
     *
     * @param exception The Firebase exception
     */
    private fun handleFirebaseError(exception: Exception) {
        val errorMessage = when (exception) {
            is FirebaseAuthInvalidUserException -> {
                // Don't reveal if email exists or not (security best practice)
                "If this email is registered, you will receive a password reset link."
            }
            else -> {
                when {
                    exception.message?.contains("network", ignoreCase = true) == true -> {
                        "Network error. Check your internet connection."
                    }
                    else -> {
                        "Failed to send reset email. Please try again."
                    }
                }
            }
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows or hides the loading indicator and disables/enables UI controls.
     *
     * @param isLoading True to show loading, false to hide
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSendResetEmail.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.tvBackToLogin.isEnabled = !isLoading
    }
}
