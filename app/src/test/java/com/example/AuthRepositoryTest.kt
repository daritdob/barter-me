package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AuthRepository
import com.example.data.CredentialResult
import com.example.data.SecurePrefs
import com.example.data.VerificationCodeManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthRepositoryTest {

    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securePrefs = SecurePrefs.createForTest(context, "auth_repo_test_prefs")
        authRepository = AuthRepository(context, securePrefs)
        runBlocking { authRepository.deleteAccount() }
    }

    @Test
    fun signInWithCredentials_rejectsInvalidEmail() = runBlocking {
        val result = authRepository.signInWithCredentials(
            email = "not-an-email",
            password = "password123",
            displayName = "Test User",
            isSignUp = true
        )
        assertTrue(result is CredentialResult.Error)
    }

    @Test
    fun signInWithCredentials_rejectsShortPassword() = runBlocking {
        val result = authRepository.signInWithCredentials(
            email = "user@example.com",
            password = "short",
            displayName = "Test User",
            isSignUp = true
        )
        assertTrue(result is CredentialResult.Error)
    }

    @Test
    fun localSignUp_requiresVerification() = runBlocking {
        val result = authRepository.signInWithCredentials(
            email = "trader@example.com",
            password = "password123",
            displayName = "Trader One",
            isSignUp = true
        )
        assertTrue(result is CredentialResult.Success)
        assertTrue((result as CredentialResult.Success).requiresVerification)
        assertTrue(authRepository.authState.value.isLoggedIn)
        assertFalse(authRepository.authState.value.isVerified)
        assertEquals("trader@example.com", authRepository.authState.value.email)
    }

    @Test
    fun localSignIn_withWrongPassword_fails() = runBlocking {
        authRepository.signInWithCredentials(
            email = "trader@example.com",
            password = "password123",
            displayName = "Trader One",
            isSignUp = true
        )
        authRepository.signOut()

        val result = authRepository.signInWithCredentials(
            email = "trader@example.com",
            password = "wrong-password",
            displayName = "Trader One",
            isSignUp = false
        )
        assertTrue(result is CredentialResult.Error)
    }

    @Test
    fun verifyAccount_marksUserVerified() = runBlocking {
        authRepository.signInWithCredentials(
            email = "verify@example.com",
            password = "password123",
            displayName = "Verify Me",
            isSignUp = true
        )
        val verified = authRepository.verifyAccount(VerificationCodeManager.DEBUG_FALLBACK_CODE)
        assertTrue(verified)
        assertTrue(authRepository.authState.value.isVerified)
    }

    @Test
    fun signOut_clearsSession() = runBlocking {
        authRepository.signInWithCredentials(
            email = "logout@example.com",
            password = "password123",
            displayName = "Logout User",
            isSignUp = true
        )
        authRepository.signOut()
        assertFalse(authRepository.authState.value.isLoggedIn)
        assertFalse(authRepository.authState.value.isVerified)
    }
}
