package com.nivy.wallet.android.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nivy.ui.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

// How the notification light blinks.
private const val LightOnMs = 1000
private const val LightOffMs = 200

class NotificationService @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    fun defaultNivyNotification(
        channel: NivyNotificationChannel,
        autoCancel: Boolean = true,
        priority: Int = NotificationCompat.PRIORITY_HIGH
    ): NivyNotification {
        val nivyNotification = NivyNotification(context, channel)
        val color = ContextCompat.getColor(context, R.color.green)
        nivyNotification.setSmallIcon(R.drawable.ic_notification)
            .setColor(color)
            .setPriority(priority)
            .setColorized(true)
            .setLights(color, LightOnMs, LightOffMs)
            .setAutoCancel(autoCancel)
        return nivyNotification
    }

    fun showNotification(
        notification: NotificationCompat.Builder,
        notificationId: Int
    ) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
            // Register the channel with the system
            val channel = (notification as NivyNotification).nivyChannel.create(context)

            notificationManager.createNotificationChannel(channel)
            notificationManager.notify(notificationId, notification.build())
        } catch (e: SecurityException) {
            // Some devices refuse to post without the notification permission.
            Timber.e(e)
        }
    }
}
