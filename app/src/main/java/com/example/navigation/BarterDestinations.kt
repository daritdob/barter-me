package com.example.navigation

object BarterDestinations {
    const val EXPLORE = "explore"
    const val SAVED = "saved"
    const val INBOX = "inbox"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile/{userId}"
    const val CHAT = "chat/{listingId}"

    const val PROFILE_ME = "profile/me"

    fun profileRoute(userId: String) = "profile/$userId"
    fun chatRoute(listingId: Int) = "chat/$listingId"

    val topLevelRoutes = setOf(EXPLORE, SAVED, INBOX, NOTIFICATIONS, PROFILE_ME)
}
