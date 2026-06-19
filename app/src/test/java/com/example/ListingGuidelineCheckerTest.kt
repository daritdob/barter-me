package com.example

import com.example.data.GuidelineResult
import com.example.data.ListingGuidelineChecker
import com.example.data.ListingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingGuidelineCheckerTest {

    @Test
    fun check_validOffer_passes() {
        val result = ListingGuidelineChecker.check(
            have = "Logo design for small business",
            need = "Deep clean of home office",
            categoryHave = "Design",
            categoryNeed = "Cleaning",
            description = "Happy to trade a full brand kit for a thorough office clean. Remote design work only.",
            deliveryMode = "Online",
            photoUri = null,
            profileCountry = "USA",
        )
        assertEquals(GuidelineResult.Passed, result)
    }

    @Test
    fun check_shortDescription_fails() {
        val result = ListingGuidelineChecker.check(
            have = "Logo design for small business",
            need = "Deep clean of home office",
            categoryHave = "Design",
            categoryNeed = "Cleaning",
            description = "Too short",
            deliveryMode = "Online",
            photoUri = null,
            profileCountry = "USA",
        )
        assertTrue(result is GuidelineResult.Failed)
    }

    @Test
    fun check_bannedTerm_fails() {
        val result = ListingGuidelineChecker.check(
            have = "Logo design for small business",
            need = "Deep clean of home office",
            categoryHave = "Design",
            categoryNeed = "Cleaning",
            description = "Happy to trade but cash only please for extras.",
            deliveryMode = "Online",
            photoUri = null,
            profileCountry = "USA",
        )
        assertTrue(result is GuidelineResult.Failed)
    }

    @Test
    fun check_physicalWithoutPhoto_fails() {
        val result = ListingGuidelineChecker.check(
            have = "Vintage guitar in good condition",
            need = "Deep clean of home office",
            categoryHave = "Other",
            categoryNeed = "Cleaning",
            description = "Trading my guitar locally. Must meet in person to inspect.",
            deliveryMode = "Physical In-Person Handover",
            photoUri = null,
            profileCountry = "USA",
        )
        assertTrue(result is GuidelineResult.Failed)
    }

    @Test
    fun listingStatus_constants_areStable() {
        assertEquals("APPROVED", ListingStatus.APPROVED)
        assertEquals("REJECTED", ListingStatus.REJECTED)
    }
}
