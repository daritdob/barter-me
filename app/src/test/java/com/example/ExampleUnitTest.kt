package com.example

import com.example.data.BarterRepository
import com.example.data.MatchEngine
import com.example.data.model.ListingEntity
import com.example.data.model.ProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local unit tests for core app logic.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun distanceInMiles_samePoint_isZero() {
    val distance = BarterRepository.distanceInMiles(40.7128, -74.0060, 40.7128, -74.0060)
    assertEquals(0.0, distance, 0.001)
  }

  @Test
  fun distanceInMiles_knownPair_isReasonable() {
    val distance = BarterRepository.distanceInMiles(40.7128, -74.0060, 40.7250, -74.0100)
    assertTrue(distance in 0.5..3.0)
  }

  @Test
  fun matchEngine_findsNearbySkillMatch() {
    val profile = ProfileEntity(
      userId = "me",
      name = "Alex",
      role = "Designer",
      verifyStatus = "VERIFIED",
      rating = 4.8f,
      ratingCount = 1,
      skillsOffered = "Design",
      skillsNeeded = "Photography",
      latitude = 40.7128,
      longitude = -74.0060,
      locationName = "Manhattan, NY",
      avatarUrl = "https://example.com/me.jpg",
      isMe = true
    )
    val listings = listOf(
      ListingEntity(
        id = 1,
        ownerId = "user_sarah",
        ownerName = "Sarah",
        ownerAvatar = "https://example.com/sarah.jpg",
        ownerRating = 4.9f,
        ownerRatingCount = 1,
        isOwnerVerified = true,
        haveItem = "Studio Portrait Session",
        needItem = "Cleaning",
        categoryHave = "Photography",
        categoryNeed = "Cleaning",
        description = "Swap",
        locationName = "SoHo, NY",
        latitude = 40.7250,
        longitude = -74.0100,
        timestamp = System.currentTimeMillis()
      )
    )
    val match = MatchEngine.findSkillMatch(profile, listings)
    assertNotNull(match)
    assertEquals("Photography", match?.categoryHave)
  }
}
