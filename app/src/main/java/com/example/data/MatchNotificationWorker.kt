package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.model.ChatMessageEntity
import kotlin.random.Random

class MatchNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = BarterDatabase.getDatabase(applicationContext)
        val dao = database.barterDao

        val myProfile = dao.getProfileById("me") ?: return Result.success()

        // 1. Check for Geolocation Area Trade Matches
        val allListings = dao.getAllListingsStatic()
        val matchedListing = MatchEngine.findSkillMatch(myProfile, allListings)

        if (matchedListing != null) {
            sendSystemNotification(
                title = "⚡ Local Match Found!",
                message = "${matchedListing.ownerName} offers nearby '${matchedListing.haveItem}' which matches your needs!"
            )
        } else {
            // Fallback: Simulation of automated swap proposals as new message or rating feedback
            val randomNum = Random.nextInt(100)
            if (randomNum < 40) {
                // Introduce a simulation message
                val chatList = dao.getAllMessagesStatic()
                val partnerIds = listOf("user_sarah", "user_dave", "user_emma", "user_liam")
                val randomPartnerId = partnerIds.random()
                val partner = dao.getProfileById(randomPartnerId)
                if (partner != null) {
                    val messageText = listOf(
                        "Hey Alex! Just checking if we can finalize our trade schedule?",
                        "I added a new Skill to my trust card, check it out!",
                        "Are you still interested in exchanging some skills soon?",
                        "Would you be up for a physical handover in Manhattan?"
                    ).random()

                    // Insert the message into Room
                    dao.insertMessage(
                        ChatMessageEntity(
                            listingId = 1, // Connect to Sarah's listing or general
                            senderId = partner.userId,
                            senderName = partner.name,
                            messageText = messageText,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    sendSystemNotification(
                        title = "📩 New Message from ${partner.name}",
                        message = messageText
                    )
                }
            }
        }

        return Result.success()
    }

    private fun sendSystemNotification(title: String, message: String) {
        val channelId = "barter_matches"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Match and Trade Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you of nearby exchange opportunities matching your needs"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
