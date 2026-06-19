package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ListingEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.RatingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.model.CompletedTradeEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.WalletTransactionEntity
import com.example.data.model.TradeStateEntity
import com.example.data.model.UserPreferencesEntity

@Database(
    entities = [
        ProfileEntity::class,
        ListingEntity::class,
        ChatMessageEntity::class,
        RatingEntity::class,
        CompletedTradeEntity::class,
        NotificationEntity::class,
        WalletTransactionEntity::class,
        TradeStateEntity::class,
        UserPreferencesEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class BarterDatabase : RoomDatabase() {
    abstract val barterDao: BarterDao

    companion object {
        @Volatile
        private var INSTANCE: BarterDatabase? = null

        fun getDatabase(context: Context): BarterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BarterDatabase::class.java,
                    "barter_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                
                // Active, reliable prepopulation: Runs check asynchronously right away if empty
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = instance.barterDao
                    val repository = BarterRepository(dao)
                    try {
                        repository.ensureAppStateSeeded()
                        if (dao.getProfileById("me") == null) {
                            prepopulateDatabase(dao)
                            repository.ensureAppStateSeeded()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                instance
            }
        }

        private suspend fun prepopulateDatabase(dao: BarterDao) {
            // 1. My Profile (Initial signup state)
            val myProfile = ProfileEntity(
                userId = "me",
                name = "Alex Mercer",
                role = "UI/UX Designer",
                verifyStatus = "VERIFIED",
                rating = 4.8f,
                ratingCount = 12,
                skillsOffered = "UI Designing, App Prototyping, Brand Consultation",
                skillsNeeded = "Guitar Tutoring, Portrait Photography, Catering",
                latitude = 40.7128,  // New York
                longitude = -74.0060,
                locationName = "Manhattan, NY",
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                isMe = true
            )
            dao.insertProfile(myProfile)

            // 2. Other profiles in the community
            val profileSarah = ProfileEntity(
                userId = "user_sarah",
                name = "Sarah Jenkins",
                role = "Professional Photographer",
                verifyStatus = "VERIFIED",
                rating = 4.9f,
                ratingCount = 28,
                skillsOffered = "Studio Portraiture, Product Photography, Video Editing",
                skillsNeeded = "Studio Deep Cleaning, Tax Preparation, Website Redesign",
                latitude = 40.7250,
                longitude = -74.0100,
                locationName = "SoHo, NY",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
            )
            val profileDave = ProfileEntity(
                userId = "user_dave",
                name = "David Klay",
                role = "Professional Cleaner",
                verifyStatus = "VERIFIED",
                rating = 4.7f,
                ratingCount = 19,
                skillsOffered = "Eco-Friendly Cleaning, Carpet Washing, Window Spraying",
                skillsNeeded = "Family Portraits, Logo Design, Business Card Printing",
                latitude = 40.7010,
                longitude = -74.0200,
                locationName = "Financial District, NY",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"
            )
            val profileLiam = ProfileEntity(
                userId = "user_liam",
                name = "Liam Foster",
                role = "Chef & Caterer",
                verifyStatus = "UNVERIFIED",
                rating = 4.5f,
                ratingCount = 8,
                skillsOffered = "Meal Prep, Gourmet Event Catering, Pastry Baking",
                skillsNeeded = "SEO Optimization, Cooking Assistant, Event DJ",
                latitude = 40.7580,
                longitude = -73.9850,
                locationName = "Times Square, NY",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
            )
            val profileEmma = ProfileEntity(
                userId = "user_emma",
                name = "Emma Watson",
                role = "Language Tutor",
                verifyStatus = "VERIFIED",
                rating = 5.0f,
                ratingCount = 35,
                skillsOffered = "Spanish Coaching, French Translation, Conversational English",
                skillsNeeded = "Creative Writing, Violin Lessons, Portrait Sketches",
                latitude = 40.7829,
                longitude = -73.9654,
                locationName = "Central Park, NY",
                avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150"
            )

            dao.insertProfile(profileSarah)
            dao.insertProfile(profileDave)
            dao.insertProfile(profileLiam)
            dao.insertProfile(profileEmma)

            // 3. Prepopulated seed listings
            val listing1 = ListingEntity(
                id = 1,
                ownerId = "user_sarah",
                ownerName = "Sarah Jenkins",
                ownerAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                ownerRating = 4.9f,
                ownerRatingCount = 28,
                isOwnerVerified = true,
                haveItem = "2-hour portrait session",
                needItem = "Deep clean of photo studio",
                categoryHave = "Photography",
                categoryNeed = "Cleaning",
                description = "I'll shoot and retouch 15 photos in my SoHo studio. Looking for someone to deep-clean floors, windows, and gear tables before my next client week.",
                locationName = "SoHo, NY",
                latitude = 40.7250,
                longitude = -74.0100,
                timestamp = System.currentTimeMillis() - 3600000 * 2,
                isSaved = true,
            )

            val listing2 = ListingEntity(
                id = 2,
                ownerId = "user_dave",
                ownerName = "David Klay",
                ownerAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                ownerRating = 4.7f,
                ownerRatingCount = 19,
                isOwnerVerified = true,
                haveItem = "Eco-friendly home or office clean",
                needItem = "Logo and brand kit design",
                categoryHave = "Cleaning",
                categoryNeed = "Design",
                description = "Full deep clean for a small apartment or office. Happy to trade for a modern logo plus simple color guidelines for my cleaning business.",
                locationName = "Financial District, NY",
                latitude = 40.7010,
                longitude = -74.0200,
                timestamp = System.currentTimeMillis() - 3600000 * 5,
                isSaved = false,
            )

            val listing3 = ListingEntity(
                id = 3,
                ownerId = "user_emma",
                ownerName = "Emma Watson",
                ownerAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                ownerRating = 5.0f,
                ownerRatingCount = 35,
                isOwnerVerified = true,
                haveItem = "Six Spanish conversation lessons",
                needItem = "Help fixing a portfolio website",
                categoryHave = "Education",
                categoryNeed = "Tech",
                description = "Native speaker offering six 45-minute tutoring sessions. I need help debugging a simple personal portfolio site built with basic HTML/CSS.",
                locationName = "Central Park, NY",
                latitude = 40.7829,
                longitude = -73.9654,
                timestamp = System.currentTimeMillis() - 3600000 * 12,
                isSaved = true,
            )

            val listing4 = ListingEntity(
                id = 4,
                ownerId = "user_liam",
                ownerName = "Liam Foster",
                ownerAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                ownerRating = 4.5f,
                ownerRatingCount = 8,
                isOwnerVerified = false,
                haveItem = "3-course dinner for four",
                needItem = "Social media template pack",
                categoryHave = "Catering",
                categoryNeed = "Design",
                description = "French-trained cook preparing a three-course meal at your place (groceries included). Looking for help standardizing Instagram posts and story templates.",
                locationName = "Times Square, NY",
                latitude = 40.7580,
                longitude = -73.9850,
                timestamp = System.currentTimeMillis() - 3600000 * 24,
                isSaved = false,
            )

            dao.insertListing(listing1)
            dao.insertListing(listing2)
            dao.insertListing(listing3)
            dao.insertListing(listing4)

            // 4. Initial chat history to bring the chat feature to life
            dao.insertMessage(
                ChatMessageEntity(
                    listingId = 1,
                    senderId = "user_sarah",
                    senderName = "Sarah Jenkins",
                    messageText = "Hi Alex! I saw your profile and noticed you are a skilled UI designer. Your work looks incredible!",
                    timestamp = System.currentTimeMillis() - 3600000 * 4
                )
            )
            dao.insertMessage(
                ChatMessageEntity(
                    listingId = 1,
                    senderId = "me",
                    senderName = "Alex Mercer",
                    messageText = "Thanks Sarah! Yes, I can absolutely help you design your studio website. Your portrait photography is stunning as well.",
                    timestamp = System.currentTimeMillis() - 3600000 * 3
                )
            )
            dao.insertMessage(
                ChatMessageEntity(
                    listingId = 1,
                    senderId = "user_sarah",
                    senderName = "Sarah Jenkins",
                    messageText = "Perfect, would you be open to exchanging a full homepage layout for a 3-hour photoshoot? Let me know!",
                    timestamp = System.currentTimeMillis() - 3600000 * 2
                )
            )

            // 5. Some ratings for the users to show off the secure ratings system
            dao.insertRating(
                RatingEntity(
                    listingId = 100, // mock archive listing
                    fromUserId = "user_dave",
                    fromUserName = "David Klay",
                    toUserId = "user_sarah",
                    ratingValue = 5,
                    comment = "Brilliant session! Sarah was incredibly professional and delivered gorgeous photos. Handled the shoot with absolute grace and made it very fun.",
                    timestamp = System.currentTimeMillis() - 3600000 * 48
                )
            )
            dao.insertRating(
                RatingEntity(
                    listingId = 101, // mock archive listing
                    fromUserId = "user_emma",
                    fromUserName = "Emma Watson",
                    toUserId = "user_sarah",
                    ratingValue = 5,
                    comment = "Outstanding portraits of my language students! Highly recommend her services to anyone wanting a premium photogenic feel.",
                    timestamp = System.currentTimeMillis() - 3600000 * 96
                )
            )
            dao.insertRating(
                RatingEntity(
                    listingId = 102, // mock archive listing
                    fromUserId = "me",
                    fromUserName = "Alex Mercer",
                    toUserId = "user_dave",
                    ratingValue = 5,
                    comment = "Dave was on-time, polite, and left my small designer studio spotlessly clean! Extremely professional and worth every bit of the barter.",
                    timestamp = System.currentTimeMillis() - 3600000 * 12
                )
            )

            // Seeded community reviews targeting 'me' so 'My Trust Card' is populated on load
            dao.insertRating(
                RatingEntity(
                    listingId = 103,
                    fromUserId = "user_sarah",
                    fromUserName = "Sarah Jenkins",
                    toUserId = "me",
                    ratingValue = 5,
                    comment = "Alex redesigned my promo site quickly and kept me in the loop the whole time. Great barter partner.",
                    timestamp = System.currentTimeMillis() - 3600000 * 24
                )
            )
            dao.insertRating(
                RatingEntity(
                    listingId = 104,
                    fromUserId = "user_emma",
                    fromUserName = "Emma Watson",
                    toUserId = "me",
                    ratingValue = 5,
                    comment = "Super patient! Alex coached me through setting up my Android Studio emulator, local development environment, and state flows. A brilliant barter!",
                    timestamp = System.currentTimeMillis() - 3600000 * 48
                )
            )

            // Seed Completed Trades
            dao.insertCompletedTrade(
                CompletedTradeEntity(
                    userId = "user_sarah",
                    partnerId = "user_dave",
                    partnerName = "David Klay",
                    partnerAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    barterTitle = "Studio portrait session for workspace deep cleaning",
                    ratingValue = 5,
                    ratingComment = "Brilliant session! Sarah was incredibly professional and delivered gorgeous photos. Handled the shoot with absolute grace and made it very fun.",
                    timestamp = System.currentTimeMillis() - 3600000 * 48
                )
            )

            dao.insertCompletedTrade(
                CompletedTradeEntity(
                    userId = "user_sarah",
                    partnerId = "user_emma",
                    partnerName = "Emma Watson",
                    partnerAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                    barterTitle = "Dynamic language student portraits photoshoot",
                    ratingValue = 5,
                    ratingComment = "Outstanding portraits of my language students! Highly recommend her services to anyone wanting a premium photogenic feel.",
                    timestamp = System.currentTimeMillis() - 3600000 * 96
                )
            )

            dao.insertCompletedTrade(
                CompletedTradeEntity(
                    userId = "user_dave",
                    partnerId = "me",
                    partnerName = "Alex Mercer",
                    partnerAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    barterTitle = "Eco-Friendly deep cleaning for mobile UI design guidelines",
                    ratingValue = 5,
                    ratingComment = "Dave was on-time, polite, and left my small designer studio spotlessly clean! Extremely professional and worth every bit of the barter.",
                    timestamp = System.currentTimeMillis() - 3600000 * 12
                )
            )

            dao.insertCompletedTrade(
                CompletedTradeEntity(
                    userId = "me",
                    partnerId = "user_sarah",
                    partnerName = "Sarah Jenkins",
                    partnerAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    barterTitle = "Responsive online photo gallery design for personal photoshoot studio coupon",
                    ratingValue = 5,
                    ratingComment = "Sarah was so insightful! Alex structured the website aesthetics and design code incredibly fast, and helped style my photo portfolio nicely. A stellar barter experience.",
                    timestamp = System.currentTimeMillis() - 3600000 * 72
                )
            )

            dao.insertCompletedTrade(
                CompletedTradeEntity(
                    userId = "me",
                    partnerId = "user_emma",
                    partnerName = "Emma Watson",
                    partnerAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                    barterTitle = "Beginner Spanish conversation course for basic Android Jetpack Compose coding guidance",
                    ratingValue = 5,
                    ratingComment = "Fantastic! Alex is super patient. He coached me through setting up my Android Studio emulator, local development environment, and understanding State flows. Highly recommend!",
                    timestamp = System.currentTimeMillis() - 3600000 * 120
                )
            )
        }
    }
}
