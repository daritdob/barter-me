package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SecurePrefs
import com.example.data.VerificationCodeManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VerificationCodeManagerTest {

    private lateinit var manager: VerificationCodeManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager = VerificationCodeManager(SecurePrefs.createForTest(context, "verification_code_test_prefs"))
        manager.clearPending()
    }

    @Test
    fun debugFallbackCodes_areDocumented() {
        assertTrue(VerificationCodeManager.DEBUG_FALLBACK_CODE.length == 6)
        assertTrue(VerificationCodeManager.DEBUG_FALLBACK_CODE_SHORT.length == 4)
    }

    @Test
    fun issueNewCode_inDebug_returnsKnownFallback() {
        val code = manager.issueNewCode()
        assertTrue(manager.hasPendingCode())
        assertTrue(code == VerificationCodeManager.DEBUG_FALLBACK_CODE)
    }

    @Test
    fun validate_acceptsDebugFallbackCode() {
        manager.issueNewCode()
        assertTrue(manager.validate(VerificationCodeManager.DEBUG_FALLBACK_CODE))
        assertFalse(manager.hasPendingCode())
    }

    @Test
    fun validate_rejectsWrongCode() {
        manager.issueNewCode()
        assertFalse(manager.validate("000000"))
        assertTrue(manager.hasPendingCode())
    }

    @Test
    fun clearPending_removesStoredCode() {
        manager.issueNewCode()
        assertTrue(manager.hasPendingCode())
        manager.clearPending()
        assertFalse(manager.hasPendingCode())
    }
}
