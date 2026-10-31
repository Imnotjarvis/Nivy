package com.ivy.wallet.android.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ivy.ui.R
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

    fun defaultIvyNotification(
        channel: IvyNotificationChannel,
        autoCancel: Boolean = true,
        priority: Int = NotificationCompat.PRIORITY_HIGH
    ): IvyNotification {
        val ivyNotification = IvyNotification(context, channel)
        val color = ContextCompat.getColor(context, R.color.green)
        ivyNotification.setSmallIcon(R.drawable.ic_notification)
            .setColor(color)
            .setPriority(priority)
            .setColorized(true)
            .setLights(color, LightOnMs, LightOffMs)
            .setAutoCancel(autoCancel)
        return ivyNotification
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
            val channel = (notification as IvyNotification).ivyChannel.create(context)

            notificationManager.createNotificationChannel(channel)
            notificationManager.notify(notificationId, notification.build())
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
