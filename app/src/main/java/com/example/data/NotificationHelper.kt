package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.model.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NotificationHelper(
    private val context: Context,
    private val repository: BarterRepository,
    private val scope: CoroutineScope
) {
    fun showPushAndRecord(title: String, message: String) {
        showSystemNotification(title, message)
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

    private fun showSystemNotification(title: String, message: String) {
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

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify(
                (System.currentTimeMillis() % 100000).toInt(),
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
