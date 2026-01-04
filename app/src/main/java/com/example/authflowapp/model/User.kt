package com.example.authflowapp.model

/**
 * User data model for Firebase Realtime Database.
 *
 * This data class represents a user in the authentication system.
 * All properties must have default values for Firebase deserialization.
 *
 * @property uid Unique user identifier from Firebase Authentication
 * @property name Full name of the user
 * @property email User's email address
 * @property createdAt Timestamp (in milliseconds) when the user account was created
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * No-argument constructor required by Firebase Realtime Database
     * for automatic deserialization.
     */
    constructor() : this("", "", "", 0L)

    /**
     * Converts User object to a map for Firebase database operations.
     * @return Map representation of the user
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "createdAt" to createdAt
        )
    }
}
