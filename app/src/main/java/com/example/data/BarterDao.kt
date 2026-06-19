package com.example.data

import androidx.room.*
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

@Dao
interface BarterDao {

    // Profiles
    @Query("SELECT * FROM profiles WHERE isMe = 1 LIMIT 1")
    fun getMyProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfileById(userId: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    fun getProfileFlowById(userId: String): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE isMe = 0")
    fun getAllOtherProfiles(): Flow<List<ProfileEntity>>

    // Listings
    @Query("SELECT * FROM listings WHERE listingStatus = 'APPROVED' ORDER BY timestamp DESC")
    fun getPublishedListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings ORDER BY timestamp DESC")
    fun getAllListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE ownerId = :ownerId ORDER BY timestamp DESC")
    fun getListingsByOwner(ownerId: String): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE isSaved = 1 AND listingStatus = 'APPROVED' ORDER BY timestamp DESC")
    fun getSavedListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE listingStatus = 'APPROVED'")
    suspend fun getAllListingsStatic(): List<ListingEntity>

    @Query("SELECT * FROM chat_messages")
    suspend fun getAllMessagesStatic(): List<ChatMessageEntity>

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    suspend fun getListingById(id: Int): ListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: ListingEntity): Long

    @Update
    suspend fun updateListing(listing: ListingEntity)

    @Delete
    suspend fun deleteListing(listing: ListingEntity)

    // Chats
    @Query("SELECT * FROM chat_messages WHERE listingId = :listingId ORDER BY timestamp ASC")
    fun getMessagesForListing(listingId: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    // Ratings
    @Query("SELECT * FROM ratings WHERE toUserId = :toUserId ORDER BY timestamp DESC")
    fun getRatingsForUser(toUserId: String): Flow<List<RatingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: RatingEntity)

    // Completed Trades
    @Query("SELECT * FROM completed_trades WHERE userId = :userId ORDER BY timestamp DESC")
    fun getCompletedTradesForUser(userId: String): Flow<List<CompletedTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedTrade(trade: CompletedTradeEntity)

    // Notifications
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("SELECT COUNT(*) FROM app_notifications")
    suspend fun getNotificationCount(): Int

    // Wallet
    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getWalletTransactions(): Flow<List<WalletTransactionEntity>>

    @Query("SELECT COUNT(*) FROM wallet_transactions")
    suspend fun getWalletTransactionsOnce(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTransaction(transaction: WalletTransactionEntity)

    // Trade states
    @Query("SELECT * FROM trade_states")
    fun getAllTradeStates(): Flow<List<TradeStateEntity>>

    @Query("SELECT * FROM trade_states WHERE listingId = :listingId LIMIT 1")
    suspend fun getTradeState(listingId: Int): TradeStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTradeState(state: TradeStateEntity)

    // User preferences (singleton row id = 1)
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getUserPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getUserPreferencesOnce(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserPreferences(prefs: UserPreferencesEntity)
}
