package com.nivy.wallet.android.notification

import android.content.Context
import androidx.core.app.NotificationCompat

class NivyNotification(
    context: Context,
    val nivyChannel: NivyNotificationChannel
) : NotificationCompat.Builder(context, nivyChannel.channelId)
