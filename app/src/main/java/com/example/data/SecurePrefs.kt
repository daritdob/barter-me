package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Encrypted storage for auth session and verification secrets.
 */
class SecurePrefs private constructor(
    private val appContext: Context,
    private val overridePrefs: SharedPreferences?
) {

    constructor(context: Context) : this(context.applicationContext, null)

    val prefs: SharedPreferences by lazy {
        overridePrefs ?: run {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    companion object {
        private const val PREFS_FILE = "barter_auth_secure_prefs"

        /** Plain SharedPreferences for unit tests (avoids EncryptedSharedPreferences on CI). */
        fun createForTest(context: Context, name: String = "test_secure_prefs"): SecurePrefs {
            val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            return SecurePrefs(context.applicationContext, prefs)
        }
    }
}
