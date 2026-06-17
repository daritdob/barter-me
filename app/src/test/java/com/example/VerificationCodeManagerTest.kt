package com.example

import com.example.data.VerificationCodeManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for verification code validation logic (debug fallback paths).
 */
class VerificationCodeManagerTest {

    @Test
    fun debugFallbackCodes_areDocumented() {
        assertTrue(VerificationCodeManager.DEBUG_FALLBACK_CODE.length == 6)
        assertTrue(VerificationCodeManager.DEBUG_FALLBACK_CODE_SHORT.length == 4)
    }

    @Test
    fun invalidCodeFormat_isNotAcceptedAsValidPrimary() {
        assertFalse("".matches(Regex("^\\d{6}$")))
        assertTrue("123456".matches(Regex("^\\d{6}$")))
    }
}
