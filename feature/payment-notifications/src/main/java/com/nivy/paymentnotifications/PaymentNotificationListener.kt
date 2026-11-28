package com.nivy.paymentnotifications

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.repository.PaymentNotificationRepository
import com.nivy.domain.notification.CapturePaymentNotificationUseCase
import com.nivy.domain.notification.NotificationAccess
import com.nivy.domain.notification.PostedNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

/**
 * Reads the notifications of the apps picked for payment notifications, as they're posted: for
 * any other app's, it only looks at who posted it. Each one is read and kept at once, as Android
 * may freeze an idle listener between notifications. Android keeps the access under this class's
 * name ([NotificationAccess.ListenerClass]): renaming or moving it takes the access away.
 */
@AndroidEntryPoint
class PaymentNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var capture: CapturePaymentNotificationUseCase

    @Inject
    lateinit var access: NotificationAccess

    @Inject
    lateinit var repository: PaymentNotificationRepository

    @Inject
    lateinit var dataObserver: DataObserver

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // The apps picked, by package name, with the names they were picked with.
    @Volatile
    private var picked: Map<String, String?> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            // Apps picked or removed, or a backup restored.
            dataObserver.writeEvents
                .filter { it is DataWriteEvent.PaymentNotificationsChanged || it is DataWriteEvent.AllDataChange }
                .collect { loadPicked() }
        }
    }

    override fun onListenerConnected() {
        access.setConnected(true)
        scope.launch {
            loadPicked()
            // What's still in the shade, posted while Nivy wasn't listening; the ones read already
            // are skipped.
            val shown = runCatching { activeNotifications.orEmpty().toList() }.getOrDefault(emptyList())
            shown.filter(::isPayment).forEach { read(it) }
        }
    }

    override fun onListenerDisconnected() {
        access.setConnected(false)
        access.rebind()
    }

    override fun onNotificationPosted(notification: StatusBarNotification) {
        scope.launch { access.heard() }
        when {
            notification.packageName == packageName -> {
                if (notification.tag == NotificationAccess.TestTag) access.testWasHeard()
            }
            isPayment(notification) -> scope.launch { read(notification) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // A picked app's notification, not an ongoing one or a group's summary.
    private fun isPayment(notification: StatusBarNotification) =
        notification.packageName in picked &&
            !notification.isOngoing &&
            notification.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0

    private suspend fun loadPicked() {
        picked = repository.findSources()
            .filter { it.kind == PaymentSourceEntity.App }
            .associate { it.id to it.name }
    }

    private suspend fun read(notification: StatusBarNotification) {
        val extras = notification.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n")
        runCatching {
            capture.capture(
                PostedNotification(
                    packageName = notification.packageName,
                    appName = picked[notification.packageName],
                    key = notification.key,
                    postTime = Instant.ofEpochMilli(notification.postTime),
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                    text = text?.toString(),
                )
            )
        }.onFailure(Timber::e)
    }
}

/**
 * After the phone restarts, or Nivy updates, asks Android to bind the listener again. Only those
 * two system broadcasts: any other intent, or one without an action, does nothing.
 */
class ListenerRebindReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RebindOn) NotificationAccess.rebind(context)
    }

    private companion object {
        val RebindOn = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
