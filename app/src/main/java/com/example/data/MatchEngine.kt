package com.example.data

import com.example.data.model.ListingEntity
import com.example.data.model.ProfileEntity

object MatchEngine {

    private const val DEFAULT_MAX_MILES = 10.0

    fun findSkillMatch(
        profile: ProfileEntity,
        listings: List<ListingEntity>,
        maxMiles: Double = DEFAULT_MAX_MILES
    ): ListingEntity? {
        return listings.firstOrNull { listing ->
            listing.ownerId != profile.userId &&
                (
                    profile.skillsNeeded.contains(listing.categoryHave, ignoreCase = true) ||
                        profile.skillsNeeded.contains(listing.haveItem, ignoreCase = true)
                    ) &&
                BarterRepository.distanceInMiles(
                    profile.latitude, profile.longitude,
                    listing.latitude, listing.longitude
                ) <= maxMiles
        }
    }

    fun filterListings(
        listings: List<ListingEntity>,
        query: String,
        category: String?,
        maxDist: Float?,
        profile: ProfileEntity?
    ): List<ListingEntity> {
        return listings.filter { listing ->
            val matchesQuery = query.isEmpty() ||
                listing.haveItem.contains(query, ignoreCase = true) ||
                listing.needItem.contains(query, ignoreCase = true) ||
                listing.description.contains(query, ignoreCase = true) ||
                listing.ownerName.contains(query, ignoreCase = true)

            val matchesCategory = category == null ||
                listing.categoryHave.equals(category, ignoreCase = true) ||
                listing.categoryNeed.equals(category, ignoreCase = true)

            val matchesDistance = if (maxDist != null && profile != null) {
                BarterRepository.distanceInMiles(
                    profile.latitude, profile.longitude,
                    listing.latitude, listing.longitude
                ) <= maxDist
            } else {
                true
            }

            matchesQuery && matchesCategory && matchesDistance
        }
    }

    fun matchesSkills(profile: ProfileEntity, listing: ListingEntity): Boolean {
        return profile.skillsNeeded.contains(listing.categoryHave, ignoreCase = true) ||
            profile.skillsNeeded.contains(listing.haveItem, ignoreCase = true)
    }

    fun isWithinRadius(
        profile: ProfileEntity,
        listing: ListingEntity,
        maxMiles: Double = DEFAULT_MAX_MILES
    ): Boolean {
        return BarterRepository.distanceInMiles(
            profile.latitude, profile.longitude,
            listing.latitude, listing.longitude
        ) <= maxMiles
    }
}
