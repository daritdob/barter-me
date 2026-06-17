package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.BarterDatabase
import com.example.data.BarterRepository
import com.example.data.CredentialResult
import com.example.data.MatchEngine
import com.example.data.NotificationHelper
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CompletedTradeEntity
import com.example.data.model.ListingEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.RatingEntity
import com.example.data.model.UserPreferencesEntity
import com.example.data.model.WalletTransactionEntity
import com.example.BuildConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

typealias AppNotification = NotificationEntity

class BarterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BarterRepository
    private val authRepository: AuthRepository
    private val notificationHelper: NotificationHelper

    val isLoggedIn: StateFlow<Boolean> = authRepository.authState.map { it.isLoggedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isProUser: StateFlow<Boolean> = authRepository.authState.map { it.isProUser }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isVerified: StateFlow<Boolean> = authRepository.authState.map { it.isVerified }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val inAppVerificationCode: StateFlow<String?> = authRepository.inAppVerificationCode
    val isFirebaseAuthEnabled: Boolean get() = authRepository.isFirebaseEnabled
    val usesFirebaseAuth: StateFlow<Boolean> = authRepository.authState.map { it.usesFirebase }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isSocialVerificationSelfServiceEnabled: Boolean get() = BuildConfig.DEBUG

    private val preferences: StateFlow<UserPreferencesEntity> =
        repository.userPreferences.map { it ?: UserPreferencesEntity() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesEntity())

    val isDarkMode: StateFlow<Boolean> = preferences.map { it.isDarkMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val maxDistanceFilter: StateFlow<Float?> = preferences.map { it.maxDistanceFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val searchQuery: StateFlow<String> = preferences.map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedCategory: StateFlow<String?> = preferences.map { it.selectedCategory }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val walletBalance: StateFlow<Int> = preferences.map { it.walletBalance }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4200)

    val inAppNotifications: StateFlow<List<NotificationEntity>> =
        repository.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerTransactions: StateFlow<List<WalletTransactionEntity>> =
        repository.walletTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    enum class TradeState {
        NEGOTIATING, AGREEMENT_SIGNED, IN_PROGRESS, UNDER_REVIEW, COMPLETED
    }

    val tradeLifecycleStates: StateFlow<Map<Int, TradeState>> =
        repository.tradeStates.map { map ->
            map.mapValues { (_, value) -> TradeState.valueOf(value) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _activeChatListingId = MutableStateFlow<Int?>(null)
    val activeChatListingId: StateFlow<Int?> = _activeChatListingId.asStateFlow()

    val myProfile: StateFlow<ProfileEntity?>
    val allListings: StateFlow<List<ListingEntity>>
    val savedListings: StateFlow<List<ListingEntity>>
    val otherProfiles: StateFlow<List<ProfileEntity>>
    val allChatMessages: StateFlow<List<ChatMessageEntity>>

    data class ChatThread(
        val listing: ListingEntity,
        val lastMessage: ChatMessageEntity
    )

    val chatThreads: StateFlow<List<ChatThread>>
    val filteredListings: StateFlow<List<ListingEntity>>

    init {
        val database = BarterDatabase.getDatabase(application)
        repository = BarterRepository(database.barterDao)
        authRepository = AuthRepository(application)
        notificationHelper = NotificationHelper(application, repository, viewModelScope)

        myProfile = repository.myProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        allListings = repository.allListings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        savedListings = repository.savedListings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        otherProfiles = repository.otherProfiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allChatMessages = repository.allChatMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        chatThreads = combine(repository.allChatMessages, repository.allListings) { messages, listings ->
            messages.groupBy { it.listingId }
                .mapNotNull { (listingId, msgList) ->
                    val listing = listings.firstOrNull { it.id == listingId } ?: return@mapNotNull null
                    val lastMsg = msgList.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                    ChatThread(listing, lastMsg)
                }
                .sortedByDescending { it.lastMessage.timestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        filteredListings = combine(allListings, preferences, myProfile) { listings, prefs, profile ->
            MatchEngine.filterListings(
                listings = listings,
                query = prefs.searchQuery,
                category = prefs.selectedCategory,
                maxDist = prefs.maxDistanceFilter,
                profile = profile
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            try {
                val currentRatings = repository.getRatingsForUser("me").first()
                if (currentRatings.isEmpty()) {
                    repository.addRating(
                        listingId = 103,
                        fromUserId = "user_sarah",
                        fromUserName = "Sarah Jenkins",
                        toUserId = "me",
                        ratingValue = 5,
                        comment = "Amazing experience redesigning my photoshoot promo site! Alex is a elite UI designer and extremely proactive. Highly recommended!"
                    )
                    repository.addRating(
                        listingId = 104,
                        fromUserId = "user_emma",
                        fromUserName = "Emma Watson",
                        toUserId = "me",
                        ratingValue = 5,
                        comment = "Super patient! Alex coached me through setting up my Android Studio emulator, local development environment, and state flows. A brilliant barter!"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun signInWithCredentials(
        email: String,
        password: String,
        displayName: String,
        isSignUp: Boolean,
        onResult: (success: Boolean, error: String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            when (val result = authRepository.signInWithCredentials(email, password, displayName, isSignUp)) {
                is CredentialResult.Success -> {
                    if (authRepository.isAdminLogin()) {
                        myProfile.value?.let { profile ->
                            repository.updateProfile(
                                profile.copy(
                                    name = AuthRepository.DEBUG_ADMIN_NAME,
                                    verifyStatus = "VERIFIED",
                                    role = "System Administrator",
                                    locationName = "Admin HQ, NY"
                                )
                            )
                        }
                    } else if (result.requiresVerification) {
                        notificationHelper.showPushAndRecord(
                            "🔐 Verification Required",
                            "Complete account verification in the Barter-me app to start trading."
                        )
                    }
                    onResult(true, null)
                }
                is CredentialResult.Error -> onResult(false, result.message)
            }
        }
    }

    /** Debug-only quick sign-in for mock social providers. */
    fun debugSocialSignIn(email: String, name: String, method: String) {
        if (!BuildConfig.DEBUG) return
        signInWithCredentials(email, "password1234", name, isSignUp = true)
    }

    fun resendVerification(onResult: (success: Boolean, error: String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            when (val result = authRepository.resendVerification()) {
                is CredentialResult.Success -> {
                    notificationHelper.showPushAndRecord(
                        "🔐 Verification Reminder",
                        "Open Barter-me to complete your account verification."
                    )
                    onResult(true, null)
                }
                is CredentialResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun verifyAccount(code: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val verified = if (authRepository.authState.value.usesFirebase) {
                authRepository.refreshFirebaseVerificationStatus()
            } else {
                authRepository.verifyAccount(code)
            }
            if (verified) {
                myProfile.value?.let { repository.updateProfile(it.copy(verifyStatus = "VERIFIED")) }
            }
            onResult(verified)
        }
    }

    fun signOutUser() {
        authRepository.signOut()
        viewModelScope.launch {
            myProfile.value?.let { repository.updateProfile(it.copy(verifyStatus = "UNVERIFIED")) }
        }
    }

    fun deleteUserAccount() {
        viewModelScope.launch {
            authRepository.deleteAccount()
            allListings.value.filter { it.ownerId == "me" }.forEach { repository.deleteListing(it) }
            repository.updateProfile(
                ProfileEntity(
                    userId = "me",
                    name = "Alex Mercer",
                    role = "UI/UX Designer",
                    verifyStatus = "UNVERIFIED",
                    rating = 4.8f,
                    ratingCount = 12,
                    skillsOffered = "UI Designing, App Prototyping, Brand Consultation",
                    skillsNeeded = "Guitar Tutoring, Portrait Photography, Catering",
                    latitude = 40.7128,
                    longitude = -74.0060,
                    locationName = "Manhattan, NY",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    isMe = true
                )
            )
        }
    }

    fun upgradeToPro() {
        authRepository.upgradeToPro()
        notificationHelper.showPushAndRecord(
            "👑 Pro Tier Activated!",
            "Thank you for your premium upgrade! Priority placement & swap badges are now online."
        )
    }

    fun buyCredits(credits: Int, priceUSD: Double) {
        viewModelScope.launch {
            val newBalance = walletBalance.value + credits
            repository.updateWalletBalance(newBalance)
            repository.insertWalletTransaction(
                WalletTransactionEntity(
                    title = "Credit Top-Up via Secure In-App Purchase ($${String.format("%.2f", priceUSD)})",
                    amount = credits,
                    type = "earned",
                    timestamp = System.currentTimeMillis()
                )
            )
            notificationHelper.showPushAndRecord(
                "💰 Credits Loaded!",
                "Successfully added $credits credits to your cooperative wallet. Secure ledger updated."
            )
        }
    }

    fun transferCredits(title: String, amount: Int, type: String) {
        viewModelScope.launch {
            val newBalance = if (type == "spent") {
                (walletBalance.value - amount).coerceAtLeast(0)
            } else {
                walletBalance.value + amount
            }
            repository.updateWalletBalance(newBalance)
            repository.insertWalletTransaction(
                WalletTransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
            )
            notificationHelper.showPushAndRecord(
                "Ledger Updated",
                "Transaction completed: $title. Wallet balance: $newBalance credits."
            )
        }
    }

    fun updateTradeState(listingId: Int, state: TradeState) {
        viewModelScope.launch {
            repository.upsertTradeState(listingId, state.name)
            notificationHelper.showPushAndRecord(
                "Barter Agreement Status",
                "Exchange status: ${state.name.replace("_", " ")}"
            )
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val current = preferences.value
            repository.updateUserPreferences(current.copy(isDarkMode = !current.isDarkMode))
        }
    }

    fun setMaxDistance(miles: Float?) {
        viewModelScope.launch {
            repository.updateUserPreferences(preferences.value.copy(maxDistanceFilter = miles))
        }
    }

    fun setSearchQuery(query: String) {
        viewModelScope.launch {
            repository.updateUserPreferences(preferences.value.copy(searchQuery = query))
        }
    }

    fun setCategoryFilter(category: String?) {
        viewModelScope.launch {
            repository.updateUserPreferences(preferences.value.copy(selectedCategory = category))
        }
    }

    fun setActiveChat(listingId: Int?) {
        _activeChatListingId.value = listingId
    }

    fun toggleSaveListing(listing: ListingEntity) {
        viewModelScope.launch {
            repository.updateListing(listing.copy(isSaved = !listing.isSaved))
        }
    }

    fun updateMyProfile(
        name: String,
        role: String,
        skillsHave: String,
        skillsWant: String,
        locName: String,
        isOnline: Boolean = true,
        isDndMode: Boolean = false,
        autoReply: String = "Hey there! I am on DND or offline, but will review as soon as I'm back.",
        prefCategory: String = "All"
    ) {
        viewModelScope.launch {
            val oldProfile = myProfile.value ?: return@launch
            val updated = oldProfile.copy(
                name = name,
                role = role,
                skillsOffered = skillsHave,
                skillsNeeded = skillsWant,
                locationName = locName,
                isOnline = isOnline,
                isDndMode = isDndMode,
                autoReplyMessage = autoReply,
                preferredCategory = prefCategory
            )
            repository.updateProfile(updated)
            triggerMatchingCheck(updated)
        }
    }

    fun toggleSocialVerification() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            val profile = myProfile.value ?: return@launch
            val newStatus = if (profile.verifyStatus == "VERIFIED") "UNVERIFIED" else "VERIFIED"
            repository.updateProfile(profile.copy(verifyStatus = newStatus))
        }
    }

    fun submitNewListing(
        have: String,
        need: String,
        categoryHave: String,
        categoryNeed: String,
        desc: String,
        haveType: String = "Service",
        needType: String = "Service",
        deliveryMode: String = "Online",
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val me = myProfile.value ?: return@launch
            repository.addListing(
                ListingEntity(
                    ownerId = me.userId,
                    ownerName = me.name,
                    ownerAvatar = me.avatarUrl,
                    ownerRating = me.rating,
                    ownerRatingCount = me.ratingCount,
                    isOwnerVerified = me.verifyStatus == "VERIFIED",
                    haveItem = have,
                    needItem = need,
                    categoryHave = categoryHave,
                    categoryNeed = categoryNeed,
                    description = desc,
                    locationName = me.locationName,
                    latitude = me.latitude,
                    longitude = me.longitude,
                    timestamp = System.currentTimeMillis(),
                    haveType = haveType,
                    needType = needType,
                    deliveryMode = deliveryMode,
                    countryRestricted = me.country,
                    photoUri = photoUri
                )
            )
            notificationHelper.showPushAndRecord(
                "Offer Published!",
                "Your listing '$have' has been broadcasted to barterers near ${me.locationName}!"
            )
        }
    }

    fun withdrawCredits(amount: Int, method: String, details: String): Boolean {
        if (walletBalance.value < amount || amount <= 0) return false
        viewModelScope.launch {
            val newBalance = walletBalance.value - amount
            repository.updateWalletBalance(newBalance)
            repository.insertWalletTransaction(
                WalletTransactionEntity(
                    title = "Credit Withdrawal to $method ($details)",
                    amount = -amount,
                    type = "spent",
                    timestamp = System.currentTimeMillis()
                )
            )
            notificationHelper.showPushAndRecord(
                "🏦 Withdrawal Initiated!",
                "Successfully routed $amount credits of your wallet balance to standard $method queue."
            )
        }
        return true
    }

    fun getMessagesForListing(listingId: Int): Flow<List<ChatMessageEntity>> =
        repository.getMessagesForListing(listingId)

    fun sendChatMessage(listingId: Int, recipientName: String, messageText: String) {
        viewModelScope.launch {
            val me = myProfile.value ?: return@launch
            repository.sendMessage(listingId, me.userId, me.name, messageText)

            val listing = allListings.value.firstOrNull { it.id == listingId }
            if (listing != null && listing.ownerId != me.userId) {
                val partner = repository.getProfileById(listing.ownerId)
                if (partner != null && (!partner.isOnline || partner.isDndMode)) {
                    kotlinx.coroutines.delay(1200)
                    repository.sendMessage(
                        listingId = listingId,
                        senderId = partner.userId,
                        senderName = partner.name,
                        messageText = "[Auto-Reply] ${partner.autoReplyMessage}"
                    )
                }
            }
        }
    }

    fun submitRating(listingId: Int, toUserId: String, stars: Int, comment: String, barterTitle: String = "") {
        viewModelScope.launch {
            val me = myProfile.value ?: return@launch
            repository.addRating(listingId, me.userId, me.name, toUserId, stars, comment)

            val finalTitle = barterTitle.ifBlank { "Dynamic Peer Barter Exchange Agreement" }
            repository.addCompletedTrade(
                CompletedTradeEntity(
                    userId = toUserId,
                    partnerId = me.userId,
                    partnerName = me.name,
                    partnerAvatar = me.avatarUrl,
                    barterTitle = finalTitle,
                    ratingValue = stars,
                    ratingComment = comment,
                    timestamp = System.currentTimeMillis()
                )
            )
            repository.getProfileById(toUserId)?.let { targetUser ->
                repository.addCompletedTrade(
                    CompletedTradeEntity(
                        userId = me.userId,
                        partnerId = toUserId,
                        partnerName = targetUser.name,
                        partnerAvatar = targetUser.avatarUrl,
                        barterTitle = finalTitle,
                        ratingValue = stars,
                        ratingComment = "Reviewed $stars stars: $comment",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            notificationHelper.showPushAndRecord(
                "Feedback Secured",
                "Your $stars-star review has been verified & recorded! Trust indices updated."
            )
        }
    }

    fun getReviewsForUser(userId: String): Flow<List<RatingEntity>> = repository.getRatingsForUser(userId)
    fun getCompletedTradesForUser(userId: String): Flow<List<CompletedTradeEntity>> =
        repository.getCompletedTradesForUser(userId)
    fun getProfileFlowById(userId: String): Flow<ProfileEntity?> = repository.getProfileFlowById(userId)

    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun triggerPushNotification(title: String, message: String) {
        notificationHelper.showPushAndRecord(title, message)
    }

    private fun triggerMatchingCheck(profile: ProfileEntity) {
        viewModelScope.launch {
            val match = MatchEngine.findSkillMatch(profile, allListings.value)
            if (match != null) {
                val notifTitle = "⚡ Match Match Match!"
                val notifText = "${match.ownerName} offers '${match.haveItem}' which matches your needs. Swap now!"
                notificationHelper.showPushAndRecord(notifTitle, notifText)
            }
        }
    }

    fun getDistanceTo(listing: ListingEntity): String {
        val me = myProfile.value ?: return "Unknown distance"
        val miles = repository.calculateDistanceInMiles(
            me.latitude, me.longitude, listing.latitude, listing.longitude
        )
        return "%.1f miles".format(miles)
    }

    fun getDistanceInMiles(listing: ListingEntity): Double {
        val me = myProfile.value ?: return 0.0
        return repository.calculateDistanceInMiles(
            me.latitude, me.longitude, listing.latitude, listing.longitude
        )
    }
}
