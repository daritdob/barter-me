package com.example.data

import android.content.Context
import android.util.Patterns
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

data class AuthState(
    val isLoggedIn: Boolean = false,
    val isVerified: Boolean = false,
    val isProUser: Boolean = false,
    val email: String? = null,
    val name: String? = null,
    val loginMethod: String? = null,
    val usesFirebase: Boolean = false
)

sealed class CredentialResult {
    data class Success(val requiresVerification: Boolean) : CredentialResult()
    data class Error(val message: String) : CredentialResult()
}

class AuthRepository(
    context: Context,
    securePrefsOverride: SecurePrefs? = null
) {

    private val appContext = context.applicationContext
    private val securePrefs = securePrefsOverride ?: SecurePrefs(appContext)
    private val verificationCodes = VerificationCodeManager(securePrefs)
    private val firebaseAuth = FirebaseAuthService(appContext)

    private val _authState = MutableStateFlow(readAuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Shown only on the in-app verification screen — never included in push notification bodies. */
    private val _inAppVerificationCode = MutableStateFlow<String?>(null)
    val inAppVerificationCode: StateFlow<String?> = _inAppVerificationCode.asStateFlow()

    init {
        syncFromFirebaseIfNeeded()
    }

    val isFirebaseEnabled: Boolean get() = firebaseAuth.isAvailable

    private fun readAuthState(): AuthState {
        val prefs = securePrefs.prefs
        return AuthState(
            isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false),
            isVerified = prefs.getBoolean(KEY_VERIFIED, false),
            isProUser = prefs.getBoolean(KEY_PRO_USER, false),
            email = prefs.getString(KEY_EMAIL, null),
            name = prefs.getString(KEY_NAME, null),
            loginMethod = prefs.getString(KEY_LOGIN_METHOD, null),
            usesFirebase = prefs.getBoolean(KEY_USES_FIREBASE, false)
        )
    }

    private fun persist(state: AuthState) {
        securePrefs.prefs.edit()
            .putBoolean(KEY_LOGGED_IN, state.isLoggedIn)
            .putBoolean(KEY_VERIFIED, state.isVerified)
            .putBoolean(KEY_PRO_USER, state.isProUser)
            .putBoolean(KEY_USES_FIREBASE, state.usesFirebase)
            .apply {
                if (state.email != null) putString(KEY_EMAIL, state.email) else remove(KEY_EMAIL)
                if (state.name != null) putString(KEY_NAME, state.name) else remove(KEY_NAME)
                if (state.loginMethod != null) putString(KEY_LOGIN_METHOD, state.loginMethod) else remove(KEY_LOGIN_METHOD)
            }
            .apply()
        _authState.value = state
    }

    private fun syncFromFirebaseIfNeeded() {
        if (!firebaseAuth.isAvailable) return
        val user = firebaseAuth.currentUser ?: return
        persist(
            AuthState(
                isLoggedIn = true,
                isVerified = user.isEmailVerified,
                isProUser = _authState.value.isProUser,
                email = user.email,
                name = user.displayName ?: _authState.value.name,
                loginMethod = "FIREBASE",
                usesFirebase = true
            )
        )
    }

    suspend fun signInWithCredentials(
        email: String,
        password: String,
        displayName: String,
        isSignUp: Boolean
    ): CredentialResult {
        val normalizedEmail = email.trim().lowercase()
        if (!isValidEmail(normalizedEmail)) {
            return CredentialResult.Error("Enter a valid email address")
        }
        if (!isValidPassword(password)) {
            return CredentialResult.Error("Password must be at least 8 characters")
        }

        if (BuildConfig.DEBUG && normalizedEmail == DEBUG_ADMIN_EMAIL && password == DEBUG_ADMIN_PASSWORD) {
            persist(
                AuthState(
                    isLoggedIn = true,
                    isVerified = true,
                    isProUser = _authState.value.isProUser,
                    email = normalizedEmail,
                    name = DEBUG_ADMIN_NAME,
                    loginMethod = "DEBUG_ADMIN",
                    usesFirebase = false
                )
            )
            _inAppVerificationCode.value = null
            return CredentialResult.Success(requiresVerification = false)
        }

        if (firebaseAuth.isAvailable) {
            return signInWithFirebase(normalizedEmail, password, displayName, isSignUp)
        }
        return signInLocally(normalizedEmail, password, displayName, isSignUp)
    }

    private suspend fun signInWithFirebase(
        email: String,
        password: String,
        displayName: String,
        isSignUp: Boolean
    ): CredentialResult {
        val result = if (isSignUp) {
            firebaseAuth.signUp(email, password, displayName)
        } else {
            firebaseAuth.signIn(email, password)
        }
        return when (result) {
            is AuthResult.Success -> {
                val verified = result.user.isEmailVerified
                persist(
                    AuthState(
                        isLoggedIn = true,
                        isVerified = verified,
                        isProUser = _authState.value.isProUser,
                        email = result.user.email,
                        name = result.user.displayName ?: displayName,
                        loginMethod = "FIREBASE",
                        usesFirebase = true
                    )
                )
                _inAppVerificationCode.value = null
                CredentialResult.Success(requiresVerification = !verified)
            }
            is AuthResult.Error -> CredentialResult.Error(result.message)
        }
    }

    private fun signInLocally(
        email: String,
        password: String,
        displayName: String,
        isSignUp: Boolean
    ): CredentialResult {
        val storedHash = securePrefs.prefs.getString(passwordKey(email), null)
        if (isSignUp) {
            if (storedHash != null) {
                return CredentialResult.Error("An account with this email already exists. Sign in instead.")
            }
            storePasswordHash(email, password)
        } else {
            val hash = hashPassword(password, email)
            if (storedHash == null || storedHash != hash) {
                return CredentialResult.Error("Invalid email or password")
            }
        }

        val resolvedName = if (isSignUp) displayName else (_authState.value.name ?: displayName)
        persist(
            AuthState(
                isLoggedIn = true,
                isVerified = false,
                isProUser = _authState.value.isProUser,
                email = email,
                name = resolvedName,
                loginMethod = "CREDENTIALS",
                usesFirebase = false
            )
        )
        val code = verificationCodes.issueNewCode()
        _inAppVerificationCode.value = code
        return CredentialResult.Success(requiresVerification = true)
    }

    suspend fun resendVerification(): CredentialResult {
        val state = _authState.value
        if (!state.isLoggedIn || state.isVerified) {
            return CredentialResult.Error("No verification pending")
        }
        if (state.usesFirebase) {
            return when (val result = firebaseAuth.resendEmailVerification()) {
                is AuthResult.Success -> CredentialResult.Success(requiresVerification = true)
                is AuthResult.Error -> CredentialResult.Error(result.message)
            }
        }
        val code = verificationCodes.issueNewCode()
        _inAppVerificationCode.value = code
        return CredentialResult.Success(requiresVerification = true)
    }

    suspend fun verifyAccount(code: String): Boolean {
        val state = _authState.value
        if (state.usesFirebase) {
            firebaseAuth.reloadUser()
            val verified = firebaseAuth.isEmailVerified()
            if (verified) {
                persist(state.copy(isVerified = true))
                _inAppVerificationCode.value = null
            }
            return verified
        }
        if (!verificationCodes.validate(code)) return false
        persist(state.copy(isVerified = true))
        _inAppVerificationCode.value = null
        return true
    }

    suspend fun refreshFirebaseVerificationStatus(): Boolean {
        if (!_authState.value.usesFirebase) return _authState.value.isVerified
        firebaseAuth.reloadUser()
        val verified = firebaseAuth.isEmailVerified()
        if (verified && !_authState.value.isVerified) {
            persist(_authState.value.copy(isVerified = true))
            _inAppVerificationCode.value = null
        }
        return verified
    }

    fun signOut() {
        if (_authState.value.usesFirebase) {
            firebaseAuth.signOut()
        }
        verificationCodes.clearPending()
        _inAppVerificationCode.value = null
        persist(
            AuthState(
                isLoggedIn = false,
                isVerified = false,
                isProUser = false
            )
        )
    }

    suspend fun deleteAccount() {
        val email = _authState.value.email
        if (_authState.value.usesFirebase) {
            firebaseAuth.deleteAccount()
        }
        if (email != null) {
            securePrefs.prefs.edit().remove(passwordKey(email)).apply()
        }
        securePrefs.prefs.edit().clear().apply()
        verificationCodes.clearPending()
        _inAppVerificationCode.value = null
        _authState.value = AuthState()
    }

    fun upgradeToPro() {
        val state = _authState.value.copy(isProUser = true)
        securePrefs.prefs.edit().putBoolean(KEY_PRO_USER, true).apply()
        _authState.value = state
    }

    fun isAdminLogin(): Boolean =
        BuildConfig.DEBUG && _authState.value.email == DEBUG_ADMIN_EMAIL

    fun clearInAppVerificationCode() {
        _inAppVerificationCode.value = null
    }

    private fun storePasswordHash(email: String, password: String) {
        securePrefs.prefs.edit()
            .putString(passwordKey(email), hashPassword(password, email))
            .apply()
    }

    private fun passwordKey(email: String) = "pwd_hash_$email"

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidPassword(password: String): Boolean {
        if (BuildConfig.DEBUG && password == DEBUG_ADMIN_PASSWORD) return true
        return password.length >= MIN_PASSWORD_LENGTH
    }

    companion object {
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_VERIFIED = "is_verified"
        private const val KEY_PRO_USER = "is_pro_user"
        private const val KEY_EMAIL = "logged_in_email"
        private const val KEY_NAME = "logged_in_name"
        private const val KEY_LOGIN_METHOD = "login_method"
        private const val KEY_USES_FIREBASE = "uses_firebase"

        const val DEBUG_ADMIN_EMAIL = "admin@barter.me"
        const val DEBUG_ADMIN_PASSWORD = "admin123"
        const val DEBUG_ADMIN_NAME = "Admin Tester"
        private const val MIN_PASSWORD_LENGTH = 8
    }
}
