package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NotificationHelper(
    private val context: Context,
    private val repository: BarterRepository,
    private val scope: CoroutineScope
) {
    /**
     * Posts a system notification and records it in the in-app notification center.
     *
     * @param deepLinkRoute optional navigation route (e.g. "chat/5" or "profile/me") that the
     * app should open when the notification is tapped. When null the notification simply opens
     * the app on its default destination.
     */
    fun showPushAndRecord(title: String, message: String, deepLinkRoute: String? = null) {
        showSystemNotification(title, message, deepLinkRoute)
        scope.launch {
            repository.insertNotification(
                NotificationEntity(
                    id = "sys_${System.currentTimeMillis()}",
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun showSystemNotification(title: String, message: String, deepLinkRoute: String?) {
        val channelId = CHANNEL_ID
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deepLinkRoute != null) {
                putExtra(MainActivity.EXTRA_NAV_ROUTE, deepLinkRoute)
            }
        }
        // Unique request code per notification so each PendingIntent keeps its own extras.
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(
                notificationId,
                builder.build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_ID = "barter_matches"
    }
}
