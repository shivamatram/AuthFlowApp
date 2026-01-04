package com.example.authflowapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.authflowapp.databinding.ActivityLoginBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * LoginActivity handles user authentication with Firebase.
 *
 * Session Management:
 * - Checks if user is already logged in on app launch
 * - If logged in → navigate to MainActivity
 * - If not logged in → show login screen
 *
 * Navigation Flow:
 * - "Login" button → Firebase authentication → MainActivity
 * - "Create new account" → SignupActivity
 * - "Forgot Password?" → ForgotPasswordActivity
 *
 * AdMob Integration:
 * - Displays test banner ad at bottom of screen
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Check if user is already logged in
        checkUserSession()
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AdMob
        initializeAdMob()

        setupClickListeners()
    }

    /**
     * Checks if user is already logged in.
     * If yes, navigate directly to MainActivity.
     */
    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in, go to MainActivity
            navigateToMainActivity()
        }
    }

    /**
     * Initializes Google Mobile Ads SDK and loads banner ad.
     * Production ad unit ID configured in XML.
     */
    private fun initializeAdMob() {
        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Load banner ad
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun setupClickListeners() {
        // Login button - validates and authenticates with Firebase
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            
            if (validateInputs(email, password)) {
                loginWithFirebase(email, password)
            }
        }

        // Navigate to SignupActivity
        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Navigate to ForgotPasswordActivity
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    /**
     * Validates email and password input fields.
     * Shows error messages on TextInputLayout if validation fails.
     * 
     * @param email User's email address
     * @param password User's password
     * @return true if all fields are valid, false otherwise
     */
    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        // Validate email
        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                isValid = false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Enter a valid email address"
                isValid = false
            }
            else -> {
                binding.tilEmail.error = null
            }
        }

        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    /**
     * Authenticates user with Firebase Authentication.
     * On success, navigates to MainActivity.
     * 
     * @param email User's email address
     * @param password User's password
     */
    private fun loginWithFirebase(email: String, password: String) {
        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                handleFirebaseError(exception)
            }
    }

    /**
     * Handles Firebase Authentication errors and displays user-friendly messages.
     * 
     * @param exception The Firebase exception
     */
    private fun handleFirebaseError(exception: Exception) {
        val errorMessage = when (exception) {
            is FirebaseAuthInvalidUserException -> {
                // User not found or deleted
                when (exception.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                    "ERROR_USER_DISABLED" -> "This account has been disabled."
                    else -> "Invalid user credentials."
                }
            }
            is FirebaseAuthInvalidCredentialsException -> {
                // Wrong password or malformed email
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email format."
                    "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                    else -> "Invalid credentials."
                }
            }
            else -> {
                when {
                    exception.message?.contains("network", ignoreCase = true) == true -> {
                        "Network error. Check your internet connection."
                    }
                    else -> {
                        "Login failed: ${exception.message}"
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
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.tvCreateAccount.isEnabled = !isLoading
        binding.tvForgotPassword.isEnabled = !isLoading
    }

    /**
     * Navigate to MainActivity after successful login.
     * Clears the back stack so user cannot return to login screen.
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
