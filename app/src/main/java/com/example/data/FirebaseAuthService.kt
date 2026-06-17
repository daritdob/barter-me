package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Firebase Authentication when [google-services.json] is present and Firebase is initialized.
 */
class FirebaseAuthService(context: Context) {

    private val auth: FirebaseAuth? = run {
        if (FirebaseApp.getApps(context).isEmpty()) null
        else FirebaseAuth.getInstance()
    }

    val isAvailable: Boolean get() = auth != null

    val currentUser: FirebaseUser? get() = auth?.currentUser

    fun isEmailVerified(): Boolean = auth?.currentUser?.isEmailVerified == true

    suspend fun signUp(email: String, password: String, displayName: String): AuthResult {
        val firebaseAuth = auth ?: return AuthResult.Error("Firebase is not configured")
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Account creation failed")
            val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdate).await()
            user.sendEmailVerification().await()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val firebaseAuth = auth ?: return AuthResult.Error("Firebase is not configured")
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Sign in failed")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    suspend fun resendEmailVerification(): AuthResult {
        val user = auth?.currentUser ?: return AuthResult.Error("No signed-in user")
        return try {
            user.sendEmailVerification().await()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not resend verification email")
        }
    }

    suspend fun reloadUser(): Boolean {
        return try {
            auth?.currentUser?.reload()?.await()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun signOut() {
        auth?.signOut()
    }

    suspend fun deleteAccount(): AuthResult {
        val user = auth?.currentUser ?: return AuthResult.Error("No signed-in user")
        return try {
            user.delete().await()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not delete account")
        }
    }
}
