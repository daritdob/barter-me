package com.example

import com.example.data.MatchEngine
import com.example.data.model.ListingEntity
import com.example.data.model.ProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchEngineTest {

    private val profile = ProfileEntity(
        userId = "me",
        name = "Alex",
        role = "Designer",
        verifyStatus = "VERIFIED",
        rating = 4.8f,
        ratingCount = 10,
        skillsOffered = "Design, Branding",
        skillsNeeded = "Photography, Guitar",
        latitude = 40.7128,
        longitude = -74.0060,
        locationName = "NYC",
        avatarUrl = "",
        isMe = true
    )

    private val complementaryListing = ListingEntity(
        id = 1,
        ownerId = "user_sarah",
        ownerName = "Sarah",
        ownerAvatar = "",
        ownerRating = 4.9f,
        ownerRatingCount = 5,
        isOwnerVerified = true,
        haveItem = "Portrait Photography",
        needItem = "Logo Design",
        categoryHave = "Photography",
        categoryNeed = "Design",
        description = "Photo sessions for local clients in Brooklyn.",
        locationName = "Brooklyn",
        latitude = 40.6782,
        longitude = -73.9442,
        timestamp = 1L,
        listingStatus = "APPROVED",
    )

    @Test
    fun findComplementaryMatches_returnsSkillMatches() {
        val matches = MatchEngine.findComplementaryMatches(
            profile,
            listOf(complementaryListing)
        )
        assertEquals(1, matches.size)
        assertEquals("Sarah", matches.first().ownerName)
    }

    @Test
    fun findComplementaryMatches_excludesOwnListings() {
        val ownListing = complementaryListing.copy(ownerId = "me")
        val matches = MatchEngine.findComplementaryMatches(profile, listOf(ownListing))
        assertTrue(matches.isEmpty())
    }
}
