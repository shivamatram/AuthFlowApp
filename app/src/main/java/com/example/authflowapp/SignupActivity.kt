package com.example.authflowapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.authflowapp.databinding.ActivitySignupBinding
import com.example.authflowapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase

/**
 * SignupActivity handles user registration with Firebase Authentication
 * and stores user data in Firebase Realtime Database.
 *
 * Navigation Flow:
 * - Opened from LoginActivity
 * - After successful signup → MainActivity
 * - "Already have account" link → back to LoginActivity
 */
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Sign Up button - validates and creates Firebase user
        binding.btnSignup.setOnClickListener {
            if (validateInputs()) {
                val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
                val email = binding.etEmail.text?.toString()?.trim().orEmpty()
                val password = binding.etPassword.text?.toString().orEmpty()
                
                createUserWithFirebase(fullName, email, password)
            }
        }

        // Navigate back to LoginActivity
        binding.tvAlreadyHaveAccount.setOnClickListener {
            finish() // Goes back to LoginActivity
        }
    }

    /**
     * Validates all input fields including password matching.
     * Shows error messages on TextInputLayout if validation fails.
     * @return true if all fields are valid, false otherwise
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        // Validate Full Name
        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        // Validate Email
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Validate Password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        // Validate Confirm Password
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }

    /**
     * Creates a new user with Firebase Authentication.
     * On success, saves user data to Realtime Database.
     *
     * @param fullName User's full name
     * @param email User's email address
     * @param password User's password
     */
    private fun createUserWithFirebase(fullName: String, email: String, password: String) {
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                // Get the newly created user's UID
                val uid = authResult.user?.uid ?: ""
                
                if (uid.isNotEmpty()) {
                    // Save user data to Realtime Database
                    saveUserToDatabase(uid, fullName, email)

                    // Debug-only sanity write to help diagnose Realtime Database rules
                    if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                        performSanityWrite(uid)
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Failed to create user", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                handleFirebaseError(exception)
            }
    }

    /**
     * Saves user data to Firebase Realtime Database under users/{uid} node.
     *
     * @param uid Firebase Authentication user ID
     * @param name User's full name
     * @param email User's email address
     */
    private fun saveUserToDatabase(uid: String, name: String, email: String) {
        val user = User(
            uid = uid,
            name = name,
            email = email,
            createdAt = System.currentTimeMillis()
        )

        database.reference
            .child("users")
            .child(uid)
            .setValue(user)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
            .addOnFailureListener { exception ->
                showLoading(false)

                // Detect permission errors and give actionable guidance
                val msg = if (exception.message?.contains("permission", ignoreCase = true) == true
                    || exception.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                    "Account created but failed to save profile due to database permissions.\n" +
                        "Please deploy the Realtime Database rules and ensure authenticated users can write to /users/{uid}."
                } else {
                    "Account created but failed to save data: ${exception.message}"
                }

                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

                // Conditional debug log to help with diagnostics (stripped in release by ProGuard)
                if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    android.util.Log.e("SignupActivity", "Failed to write user to database", exception)
                }

                // Still navigate to MainActivity since auth succeeded
                navigateToMainActivity()
            }
    }

    /**
     * Handles Firebase Authentication errors and displays user-friendly messages.
     *
     * @param exception The Firebase exception
     */
    private fun handleFirebaseError(exception: Exception) {
        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException -> {
                "Password is too weak. Use at least 6 characters."
            }
            is FirebaseAuthInvalidCredentialsException -> {
                "Invalid email format."
            }
            is FirebaseAuthUserCollisionException -> {
                "This email is already registered. Please login."
            }
            else -> {
                when {
                    exception.message?.contains("network", ignoreCase = true) == true -> {
                        "Network error. Check your internet connection."
                    }
                    else -> {
                        "Signup failed: ${exception.message}"
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
        binding.btnSignup.isEnabled = !isLoading
        binding.etFullName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.tvAlreadyHaveAccount.isEnabled = !isLoading
    }

    /**
     * Navigate to MainActivity after successful signup.
     * Clears the back stack so user cannot return to signup screen.
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Debug helper: Attempt a small write to /users/{uid}/_sanity_test to verify rules.
     * Shows toast + logs for success/failure (only in debuggable builds).
     */
    private fun performSanityWrite(uid: String) {
        val testRef = database.reference.child("users").child(uid).child("_sanity_test")
        val payload = mapOf(
            "ts" to System.currentTimeMillis(),
            "device" to android.os.Build.MODEL
        )

        testRef.setValue(payload)
            .addOnSuccessListener {
                Toast.makeText(this, "Sanity write succeeded — rules allow writes.", Toast.LENGTH_SHORT).show()
                if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    android.util.Log.d("SignupActivity", "Sanity write success for uid=$uid")
                }
            }
            .addOnFailureListener { ex ->
                Toast.makeText(this, "Sanity write failed: ${ex.message}", Toast.LENGTH_LONG).show()
                if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    android.util.Log.e("SignupActivity", "Sanity write failed", ex)
                }
            }
    }
}
