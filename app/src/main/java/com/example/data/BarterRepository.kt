package com.example.data

import com.example.data.model.ChatMessageEntity
import com.example.data.model.ListingEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.RatingEntity
import com.example.data.model.CompletedTradeEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.WalletTransactionEntity
import com.example.data.model.TradeStateEntity
import com.example.data.model.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.*

class BarterRepository(private val barterDao: BarterDao) {

    companion object {
        /** Haversine distance in miles — single source of truth for filters and background matching. */
        fun distanceInMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 3958.8 // miles
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c
        }
    }

    val myProfile: Flow<ProfileEntity?> = barterDao.getMyProfile()
    val allListings: Flow<List<ListingEntity>> = barterDao.getPublishedListings()
    val savedListings: Flow<List<ListingEntity>> = barterDao.getSavedListings()
    val otherProfiles: Flow<List<ProfileEntity>> = barterDao.getAllOtherProfiles()
    val allChatMessages: Flow<List<ChatMessageEntity>> = barterDao.getAllChatMessages()
    val notifications: Flow<List<NotificationEntity>> = barterDao.getAllNotifications()
    val walletTransactions: Flow<List<WalletTransactionEntity>> = barterDao.getWalletTransactions()
    val tradeStates: Flow<Map<Int, String>> = barterDao.getAllTradeStates().map { states ->
        states.associate { it.listingId to it.state }
    }
    val tradeValuations: Flow<Map<Int, Pair<Int, Int>>> = barterDao.getAllTradeStates().map { states ->
        states.associate { it.listingId to (it.signedSelfValue to it.signedCounterpartyValue) }
    }
    val userPreferences: Flow<UserPreferencesEntity?> = barterDao.getUserPreferences()

    suspend fun getProfileById(userId: String): ProfileEntity? {
        return barterDao.getProfileById(userId)
    }

    fun getProfileFlowById(userId: String): Flow<ProfileEntity?> {
        return barterDao.getProfileFlowById(userId)
    }

    suspend fun updateProfile(profile: ProfileEntity) {
        barterDao.insertProfile(profile)
    }

    fun getListingsByOwner(ownerId: String): Flow<List<ListingEntity>> =
        barterDao.getListingsByOwner(ownerId)

    suspend fun addListing(listing: ListingEntity): Long {
        return barterDao.insertListing(listing)
    }

    suspend fun updateListing(listing: ListingEntity) {
        barterDao.updateListing(listing)
    }

    suspend fun deleteListing(listing: ListingEntity) {
        barterDao.deleteListing(listing)
    }

    suspend fun getListingById(id: Int): ListingEntity? {
        return barterDao.getListingById(id)
    }

    fun getMessagesForListing(listingId: Int): Flow<List<ChatMessageEntity>> {
        return barterDao.getMessagesForListing(listingId)
    }

    suspend fun sendMessage(listingId: Int, senderId: String, senderName: String, messageText: String) {
        val msg = ChatMessageEntity(
            listingId = listingId,
            senderId = senderId,
            senderName = senderName,
            messageText = messageText,
            timestamp = System.currentTimeMillis()
        )
        barterDao.insertMessage(msg)
    }

    fun getRatingsForUser(toUserId: String): Flow<List<RatingEntity>> {
        return barterDao.getRatingsForUser(toUserId)
    }

    suspend fun addRating(listingId: Int, fromUserId: String, fromUserName: String, toUserId: String, ratingValue: Int, comment: String) {
        val rating = RatingEntity(
            listingId = listingId,
            fromUserId = fromUserId,
            fromUserName = fromUserName,
            toUserId = toUserId,
            ratingValue = ratingValue,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )
        barterDao.insertRating(rating)

        // Also update destination user's cumulative rating if they exist
        val profile = barterDao.getProfileById(toUserId)
        if (profile != null) {
            val prevCount = profile.ratingCount
            val prevRating = profile.rating
            val newCount = prevCount + 1
            val newRating = ((prevRating * prevCount) + ratingValue) / newCount
            
            // Format to one decimal place nicely
            val roundedRating = (newRating * 10).roundToInt() / 10.0f
            
            barterDao.insertProfile(
                profile.copy(
                    rating = roundedRating,
                    ratingCount = newCount
                )
            )
        }
    }

    fun calculateDistanceInMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        distanceInMiles(lat1, lon1, lat2, lon2)

    fun getCompletedTradesForUser(userId: String): Flow<List<CompletedTradeEntity>> {
        return barterDao.getCompletedTradesForUser(userId)
    }

    suspend fun addCompletedTrade(trade: CompletedTradeEntity) {
        barterDao.insertCompletedTrade(trade)
    }

    suspend fun insertNotification(notification: NotificationEntity) {
        barterDao.insertNotification(notification)
    }

    suspend fun markAllNotificationsRead() {
        barterDao.markAllNotificationsRead()
    }

    suspend fun insertWalletTransaction(transaction: WalletTransactionEntity) {
        barterDao.insertWalletTransaction(transaction)
    }

    suspend fun updateWalletBalance(balance: Int) {
        val current = barterDao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        barterDao.upsertUserPreferences(current.copy(walletBalance = balance))
    }

    suspend fun updateUserPreferences(prefs: UserPreferencesEntity) {
        barterDao.upsertUserPreferences(prefs)
    }

    suspend fun upsertTradeState(
        listingId: Int,
        state: String,
        signedSelfValue: Int? = null,
        signedCounterpartyValue: Int? = null
    ) {
        // Preserve previously-signed valuations across state-only transitions; only
        // overwrite them when explicit values are supplied (e.g. at agreement signing).
        val existing = barterDao.getTradeState(listingId)
        barterDao.upsertTradeState(
            TradeStateEntity(
                listingId = listingId,
                state = state,
                signedSelfValue = signedSelfValue ?: existing?.signedSelfValue ?: 0,
                signedCounterpartyValue = signedCounterpartyValue ?: existing?.signedCounterpartyValue ?: 0
            )
        )
    }

    suspend fun ensureAppStateSeeded() {
        if (barterDao.getUserPreferencesOnce() == null) {
            barterDao.upsertUserPreferences(UserPreferencesEntity())
        }
        if (barterDao.getNotificationCount() == 0) {
            barterDao.insertNotification(
                NotificationEntity(
                    id = "notif_welcome",
                    title = "Welcome back, Alex!",
                    message = "New portrait exchange opportunity is open nearby in SoHo, NY.",
                    timestamp = System.currentTimeMillis() - 60000 * 5
                )
            )
        }
        val txCount = barterDao.getWalletTransactionsOnce()
        if (txCount == 0) {
            val now = System.currentTimeMillis()
            barterDao.insertWalletTransaction(
                WalletTransactionEntity(
                    title = "Complete Brand Redesign Package (with Sarah Jenkins)",
                    amount = 500,
                    type = "earned",
                    timestamp = now - 3600000L * 24 * 3
                )
            )
            barterDao.insertWalletTransaction(
                WalletTransactionEntity(
                    title = "Office Bookkeeping Consultation (with David Klay)",
                    amount = 200,
                    type = "spent",
                    timestamp = now - 3600000L * 24
                )
            )
        }
    }
}
