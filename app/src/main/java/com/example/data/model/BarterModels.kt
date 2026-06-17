package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val role: String,
    val verifyStatus: String, // "VERIFIED" or "UNVERIFIED"
    val rating: Float,
    val ratingCount: Int,
    val skillsOffered: String,
    val skillsNeeded: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val avatarUrl: String,
    val isMe: Boolean = false,
    val country: String = "USA", // Preapproved countries check (e.g. physical handover limited to USA)
    val isOnline: Boolean = true,
    val isDndMode: Boolean = false,
    val autoReplyMessage: String = "Hey there! I am on DND or offline, but will review as soon as I'm back.",
    val preferredCategory: String = "All"
) : Serializable

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerId: String,
    val ownerName: String,
    val ownerAvatar: String,
    val ownerRating: Float,
    val ownerRatingCount: Int,
    val isOwnerVerified: Boolean,
    val haveItem: String,       // What the creator has
    val needItem: String,       // What the creator needs in return
    val categoryHave: String,
    val categoryNeed: String,
    val description: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isSaved: Boolean = false, // Toggle for offline bookmarking
    val haveType: String = "Service", // "Product" or "Service"
    val needType: String = "Service", // "Product" or "Service"
    val deliveryMode: String = "Online", // "Online" or "Physical In-Person Handover"
    val countryRestricted: String = "USA", // Physical handovers restricted to USA
    val photoUri: String? = null // Captured photo URI for offline trade item retrieval
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: Int,
    val senderId: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long
) : Serializable

@Entity(tableName = "ratings")
data class RatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: Int,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val ratingValue: Int, // 1 to 5 stars
    val comment: String,
    val timestamp: Long
) : Serializable

@Entity(tableName = "completed_trades")
data class CompletedTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,           // User who completed the trade (whose profile this is on)
    val partnerId: String,        // Barter partner user ID
    val partnerName: String,      // Barter partner display name
    val partnerAvatar: String,    // Barter partner avatar URL
    val barterTitle: String,      // Description of the exchanged services/items
    val ratingValue: Int,         // Star rating received
    val ratingComment: String,    // Associated user rating comment
    val timestamp: Long
) : Serializable

