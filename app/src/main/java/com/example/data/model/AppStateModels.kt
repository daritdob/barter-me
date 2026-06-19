package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Int,
    val type: String, // "earned" or "spent"
    val timestamp: Long
)

@Entity(tableName = "trade_states")
data class TradeStateEntity(
    @PrimaryKey val listingId: Int,
    val state: String, // NEGOTIATING, AGREEMENT_SIGNED, IN_PROGRESS, UNDER_REVIEW, COMPLETED
    val signedSelfValue: Int = 0, // credit value the current user assigned to their own service at signing
    val signedCounterpartyValue: Int = 0 // credit value assigned to the counterparty's service at signing
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val isDarkMode: Boolean = true,
    val maxDistanceFilter: Float? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val walletBalance: Int = 4200
)

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey val userId: String, // user that has been blocked by the current user
    val blockedName: String,
    val timestamp: Long
)

@Entity(tableName = "trade_reports")
data class TradeReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: Int,
    val reportedUserId: String,
    val reportedUserName: String,
    val reason: String,
    val timestamp: Long
)
