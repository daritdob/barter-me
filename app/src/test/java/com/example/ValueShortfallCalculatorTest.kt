package com.example

import com.example.data.PartyValuation
import com.example.data.ShortfallDirection
import com.example.data.ValueShortfallCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueShortfallCalculatorTest {

    private val me = PartyValuation(partyId = "me", partyName = "You", estimatedCredits = 0)
    private val them = PartyValuation(partyId = "user_sarah", partyName = "Sarah", estimatedCredits = 0)

    @Test
    fun calculate_equalValues_isBalanced() {
        val result = ValueShortfallCalculator.calculate(
            me.copy(estimatedCredits = 1500),
            them.copy(estimatedCredits = 1500)
        )
        assertEquals(0, result.shortfall)
        assertEquals(ShortfallDirection.BALANCED, result.direction)
        assertNull(result.payer)
        assertNull(result.receiver)
    }

    @Test
    fun calculate_lowerValuedSidePays_aPaysB() {
        val result = ValueShortfallCalculator.calculate(
            me.copy(estimatedCredits = 1000),
            them.copy(estimatedCredits = 1500)
        )
        assertEquals(500, result.shortfall)
        assertEquals(ShortfallDirection.A_PAYS_B, result.direction)
        assertEquals("me", result.payer?.partyId)
        assertEquals("user_sarah", result.receiver?.partyId)
        assertTrue(ValueShortfallCalculator.isPayer(result, "me"))
        assertFalse(ValueShortfallCalculator.isPayer(result, "user_sarah"))
    }

    @Test
    fun calculate_higherValuedCounterpartyReceives_bPaysA() {
        val result = ValueShortfallCalculator.calculate(
            me.copy(estimatedCredits = 2000),
            them.copy(estimatedCredits = 1200)
        )
        assertEquals(800, result.shortfall)
        assertEquals(ShortfallDirection.B_PAYS_A, result.direction)
        assertEquals("user_sarah", result.payer?.partyId)
        assertEquals("me", result.receiver?.partyId)
        assertTrue(ValueShortfallCalculator.isPayer(result, "user_sarah"))
        assertFalse(ValueShortfallCalculator.isPayer(result, "me"))
    }

    @Test
    fun calculate_shortfallIsAlwaysAbsolute() {
        val aLower = ValueShortfallCalculator.calculate(
            me.copy(estimatedCredits = 300),
            them.copy(estimatedCredits = 900)
        )
        val bLower = ValueShortfallCalculator.calculate(
            me.copy(estimatedCredits = 900),
            them.copy(estimatedCredits = 300)
        )
        assertEquals(600, aLower.shortfall)
        assertEquals(600, bLower.shortfall)
    }

    @Test
    fun calculate_negativeValuesAreClampedToZero() {
        val result = ValueShortfallCalculator.calculate(
            me.copy(estimatedCredits = -500),
            them.copy(estimatedCredits = 400)
        )
        // -500 clamps to 0, so gap is 0 - 400 = 400 and the user (A) pays.
        assertEquals(400, result.shortfall)
        assertEquals(ShortfallDirection.A_PAYS_B, result.direction)
    }

    @Test
    fun isPayer_balancedSwap_hasNoPayer() {
        val result = ValueShortfallCalculator.calculate(
            me.copy(estimatedCredits = 1000),
            them.copy(estimatedCredits = 1000)
        )
        assertFalse(ValueShortfallCalculator.isPayer(result, "me"))
        assertFalse(ValueShortfallCalculator.isPayer(result, "user_sarah"))
    }
}
