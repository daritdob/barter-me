package com.example.data

import com.example.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Manages one-time verification codes with expiry. Codes are never logged or sent in push bodies.
 */
class VerificationCodeManager(private val securePrefs: SecurePrefs) {

    fun issueNewCode(): String {
        val code = if (BuildConfig.DEBUG) {
            DEBUG_FALLBACK_CODE
        } else {
            generateSixDigitCode()
        }
        val expiresAt = System.currentTimeMillis() + CODE_TTL_MS
        securePrefs.prefs.edit()
            .putString(KEY_PENDING_CODE_HASH, hashCode(code))
            .putLong(KEY_PENDING_EXPIRES, expiresAt)
            .apply()
        return code
    }

    fun validate(code: String): Boolean {
        if (BuildConfig.DEBUG && (code == DEBUG_FALLBACK_CODE || code == DEBUG_FALLBACK_CODE_SHORT)) {
            clearPending()
            return true
        }
        val storedHash = securePrefs.prefs.getString(KEY_PENDING_CODE_HASH, null) ?: return false
        val expiresAt = securePrefs.prefs.getLong(KEY_PENDING_EXPIRES, 0L)
        if (System.currentTimeMillis() > expiresAt) {
            clearPending()
            return false
        }
        val valid = storedHash == hashCode(code)
        if (valid) clearPending()
        return valid
    }

    fun clearPending() {
        securePrefs.prefs.edit()
            .remove(KEY_PENDING_CODE_HASH)
            .remove(KEY_PENDING_EXPIRES)
            .apply()
    }

    fun hasPendingCode(): Boolean {
        val expiresAt = securePrefs.prefs.getLong(KEY_PENDING_EXPIRES, 0L)
        return expiresAt > System.currentTimeMillis() &&
            securePrefs.prefs.getString(KEY_PENDING_CODE_HASH, null) != null
    }

    private fun generateSixDigitCode(): String {
        val value = SecureRandom().nextInt(1_000_000)
        return value.toString().padStart(6, '0')
    }

    private fun hashCode(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(code.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PENDING_CODE_HASH = "pending_code_hash"
        private const val KEY_PENDING_EXPIRES = "pending_code_expires"
        private const val CODE_TTL_MS = 10 * 60 * 1000L // 10 minutes

        const val DEBUG_FALLBACK_CODE = "123456"
        const val DEBUG_FALLBACK_CODE_SHORT = "1234"
    }
}
