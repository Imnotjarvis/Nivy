package com.nivy.paymentnotifications

import android.content.Context
import com.nivy.base.time.TimeProvider
import com.nivy.domain.notification.NotificationAccess
import com.nivy.wallet.android.notification.NotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/** How the last test went: whether the listener heard Nivy's own notification. */
enum class TestResult { Waiting, Working, Failed, NeedsNotifications }

/**
 * Nivy's test that it's listening: it posts a silent notification of its own and waits a few
 * seconds for the listener to hear it. The last result stays while Nivy runs.
 */
@Singleton
class ListeningTest @Inject constructor(
    @ApplicationContext private val context: Context,
    private val access: NotificationAccess,
    private val notificationService: NotificationService,
    private val timeProvider: TimeProvider,
) {
    private val resultNow = MutableStateFlow<TestResult?>(null)
    val result: StateFlow<TestResult?> = resultNow

    suspend fun run() {
        if (resultNow.value == TestResult.Waiting) return
        val started = timeProvider.utcNow()
        if (!PhoneSettings.postTest(context, notificationService)) {
            resultNow.value = TestResult.NeedsNotifications
            return
        }
        resultNow.value = TestResult.Waiting
        try {
            val heard = withTimeoutOrNull(Wait) { access.testHeard.first { it != null && it >= started } }
            resultNow.value = if (heard != null) TestResult.Working else TestResult.Failed
        } finally {
            if (resultNow.value == TestResult.Waiting) resultNow.value = null
            PhoneSettings.cancelTest(context)
        }
    }

    private companion object {
        val Wait = 10.seconds
    }
}
