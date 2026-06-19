package com.example.data

import kotlin.math.abs

/**
 * One side of a proposed barter and the credit value they assign to the
 * service/product they are bringing to the table.
 *
 * Pure data — intentionally free of Android/Room dependencies so the
 * settlement math stays unit-testable like [MatchEngine] and
 * [ListingGuidelineChecker].
 */
data class PartyValuation(
    val partyId: String,
    val partyName: String,
    val estimatedCredits: Int,
)

/** Who owes credits to balance a swap. */
enum class ShortfallDirection {
    /** Both sides bring equal value — a fully cashless swap. */
    BALANCED,

    /** Party A brings less value, so A tops up B with credits. */
    A_PAYS_B,

    /** Party B brings less value, so B tops up A with credits. */
    B_PAYS_A,
}

/**
 * Outcome of comparing two sides of a barter.
 *
 * [shortfall] is always non-negative (the absolute value gap). [payer] is the
 * lower-valued side that settles the gap in credits, [receiver] the
 * higher-valued side. Both are null when the swap is [ShortfallDirection.BALANCED].
 */
data class ShortfallResult(
    val shortfall: Int,
    val direction: ShortfallDirection,
    val payer: PartyValuation?,
    val receiver: PartyValuation?,
)

/**
 * Computes the credit "value shortfall" between the two sides of a barter.
 *
 * The side that brings the *lower* estimated value receives more value than it
 * gives, so it pays the difference to the higher-valued side. This keeps the
 * net value each party walks away with equal.
 */
object ValueShortfallCalculator {

    fun calculate(partyA: PartyValuation, partyB: PartyValuation): ShortfallResult {
        val valueA = partyA.estimatedCredits.coerceAtLeast(0)
        val valueB = partyB.estimatedCredits.coerceAtLeast(0)
        val shortfall = abs(valueA - valueB)

        return when {
            shortfall == 0 -> ShortfallResult(
                shortfall = 0,
                direction = ShortfallDirection.BALANCED,
                payer = null,
                receiver = null,
            )
            valueA < valueB -> ShortfallResult(
                shortfall = shortfall,
                direction = ShortfallDirection.A_PAYS_B,
                payer = partyA,
                receiver = partyB,
            )
            else -> ShortfallResult(
                shortfall = shortfall,
                direction = ShortfallDirection.B_PAYS_A,
                payer = partyB,
                receiver = partyA,
            )
        }
    }

    /** True when [partyId] is the side that must pay credits to settle the swap. */
    fun isPayer(result: ShortfallResult, partyId: String): Boolean =
        result.shortfall > 0 && result.payer?.partyId == partyId
}
