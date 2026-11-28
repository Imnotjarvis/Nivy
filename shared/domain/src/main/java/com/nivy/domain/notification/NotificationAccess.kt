package com.nivy.domain.notification

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.PaymentNotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether Nivy can read notifications, and whether it is: Android's notification access for its
 * listener, whether the listener is connected, when it last heard a notification, and when it
 * heard its own test.
 */
@Singleton
class NotificationAccess @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val repository: PaymentNotificationRepository,
    private val timeProvider: TimeProvider,
) {
    private val connectedNow = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = connectedNow

    private val testHeardAt = MutableStateFlow<Instant?>(null)
    val testHeard: StateFlow<Instant?> = testHeardAt

    @Volatile
    private var heardSaved: Instant? = null

    fun granted(): Boolean = granted(context)

    /** Asks Android to bind the listener again, as it retries only once when the binding drops. */
    fun rebind() = rebind(context)

    fun setConnected(connected: Boolean) {
        connectedNow.value = connected
    }

    /** Notes that the listener heard a notification, at most once an hour: the sign it's listening. */
    suspend fun heard() {
        val now = timeProvider.utcNow()
        if (heardSaved?.let { Duration.between(it, now) < SaveEvery } != true) {
            heardSaved = now
            appPreferences.setNotificationHeard(now)
        }
    }

    /** The listener heard Nivy's own test notification. */
    fun testWasHeard() {
        testHeardAt.value = timeProvider.utcNow()
    }

    /** Whether, and why, Nivy stopped reading notifications while apps are picked; null when it didn't. */
    suspend fun stopped(): Stopped? = stoppedListening(
        appsPicked = repository.findSources().any { it.kind == PaymentSourceEntity.App },
        granted = granted(),
        heardAt = appPreferences.notificationHeardAt(),
        now = timeProvider.utcNow(),
    )

    companion object {
        /**
         * The listener's class: Android keeps the access under its name, so it must never be renamed
         * or moved.
         */
        const val ListenerClass = "com.nivy.paymentnotifications.PaymentNotificationListener"

        /** The tag of Nivy's own test notification, which the listener reports instead of reading. */
        const val TestTag = "nivy_listening_test"

        private val SaveEvery: Duration = Duration.ofHours(1)

        fun listener(context: Context) = ComponentName(context, ListenerClass)

        fun granted(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

        fun rebind(context: Context) {
            if (granted(context)) NotificationListenerService.requestRebind(listener(context))
        }
    }
}

/** Why Nivy stopped reading notifications: its access is off, or it heard none [since] then. */
data class Stopped(val accessOff: Boolean, val since: Instant?)

// A day without hearing any app's notification: Android, or the phone, put Nivy to sleep.
private val Silence: Duration = Duration.ofDays(1)

/**
 * Whether Nivy stopped reading notifications while apps are picked: its access was turned off, or
 * it heard nothing for a day since it last heard one. Not before it heard the first.
 */
fun stoppedListening(appsPicked: Boolean, granted: Boolean, heardAt: Instant?, now: Instant): Stopped? = when {
    !appsPicked -> null
    !granted -> Stopped(accessOff = true, since = null)
    heardAt != null && Duration.between(heardAt, now) > Silence -> Stopped(accessOff = false, since = heardAt)
    else -> null
}
