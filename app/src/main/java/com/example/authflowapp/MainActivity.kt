package com.example.authflowapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.authflowapp.databinding.ActivityMainBinding
import com.example.authflowapp.model.User
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * MainActivity displays user profile information and handles logout.
 *
 * Session Management:
 * - Checks if user is logged in on activity start
 * - If not logged in → redirect to LoginActivity
 *
 * Functionality:
 * - Reads user data from Firebase Realtime Database (users/{uid})
 * - Displays user name and email
 * - Handles logout with session clearing
 *
 * AdMob Integration:
 * - Displays test banner ad at bottom of screen
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize AdMob
        initializeAdMob()

        // Check if user is logged in
        if (checkUserSession()) {
            loadUserData()
            setupClickListeners()
        }
    }

    /**
     * Checks if user is logged in.
     * If not, redirects to LoginActivity.
     * @return true if user is logged in, false otherwise
     */
    private fun checkUserSession(): Boolean {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, redirect to LoginActivity
            navigateToLogin()
            return false
        }
        return true
    }

    /**
     * Initializes Google Mobile Ads SDK and loads banner ad.
     * Production ad unit ID configured in XML.
     */
    private fun initializeAdMob() {
        // Initialize Mobile Ads SDK (can be called multiple times safely)
        MobileAds.initialize(this) {}

        // Load banner ad
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    /**
     * Loads user data from Firebase Realtime Database.
     * Reads from users/{uid} and displays name and email.
     */
    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        showLoading(true)

        database.reference
            .child("users")
            .child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    showLoading(false)

                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            displayUserInfo(user)
                        } else {
                            showError("Failed to load user data")
                        }
                    } else {
                        showError("User data not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)

                    val msg = if (error.code == DatabaseError.PERMISSION_DENIED) {
                        "Permission denied reading user data. Check Realtime Database rules and ensure the authenticated user may read /users/{uid}."
                    } else {
                        "Database error: ${error.message}"
                    }

                    showError(msg)

                    // Conditional debug log for diagnostics
                    if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                        android.util.Log.e("MainActivity", "Database read cancelled: ${error.code} - ${error.message}")
                    }
                }
            })
    }

    /**
     * Displays user information in the UI.
     * @param user The User object containing name and email
     */
    private fun displayUserInfo(user: User) {
        binding.tvUserName.text = user.name
        binding.tvUserEmail.text = user.email
        binding.cardUserInfo.visibility = View.VISIBLE
    }

    /**
     * Performs logout operation.
     * Signs out from Firebase and redirects to LoginActivity.
     */
    private fun performLogout() {
        // Sign out from Firebase
        auth.signOut()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate to LoginActivity and clear back stack
        navigateToLogin()
    }

    /**
     * Shows or hides the loading indicator.
     * @param isLoading True to show loading, false to hide
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.cardUserInfo.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.btnLogout.isEnabled = !isLoading
    }

    /**
     * Shows error message to the user.
     * @param message The error message to display
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Navigates to LoginActivity and clears the back stack.
     * Ensures user cannot navigate back to MainActivity after logout.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}