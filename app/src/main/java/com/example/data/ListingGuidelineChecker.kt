package com.example.data

object ListingStatus {
    const val PENDING_REVIEW = "PENDING_REVIEW"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

sealed class GuidelineResult {
    data object Passed : GuidelineResult()
    data class Failed(val reasons: List<String>) : GuidelineResult()
}

object ListingGuidelineChecker {

    private val bannedTerms = listOf(
        "cash only", "bitcoin", "crypto", "venmo", "paypal me",
        "drugs", "weapon", "guns", "sell for", "buy for",
    )

    fun check(
        have: String,
        need: String,
        categoryHave: String,
        categoryNeed: String,
        description: String,
        deliveryMode: String,
        photoUri: String?,
        profileCountry: String,
    ): GuidelineResult {
        val reasons = mutableListOf<String>()

        if (have.trim().length < 5) {
            reasons += "Be specific about what you're offering (at least 5 characters)."
        }
        if (need.trim().length < 5) {
            reasons += "Be specific about what you want in return (at least 5 characters)."
        }
        if (have.trim().length > 120 || need.trim().length > 120) {
            reasons += "Keep offer and want titles under 120 characters."
        }
        if (description.trim().length < 20) {
            reasons += "Add a short description so others know what to expect (at least 20 characters)."
        }
        if (have.trim().equals(need.trim(), ignoreCase = true)) {
            reasons += "Offer and want should be different."
        }
        if (categoryHave !in BARTER_CATEGORIES) {
            reasons += "Pick a valid category for what you offer."
        }
        if (categoryNeed !in BARTER_CATEGORIES) {
            reasons += "Pick a valid category for what you want."
        }

        val combined = "$have $need $description".lowercase()
        if (bannedTerms.any { combined.contains(it) }) {
            reasons += "Offers must be skill or service swaps, not sales or prohibited items."
        }

        val isPhysical = deliveryMode.contains("Handover", ignoreCase = true) ||
            deliveryMode.contains("Physical", ignoreCase = true)
        if (isPhysical && profileCountry == "USA" && photoUri.isNullOrBlank()) {
            reasons += "Add a photo for in-person item swaps."
        }

        return if (reasons.isEmpty()) GuidelineResult.Passed else GuidelineResult.Failed(reasons)
    }
}
