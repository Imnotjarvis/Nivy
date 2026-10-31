package com.ivy.wallet.domain.logic.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ivy.base.time.TimeProvider
import com.ivy.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Keeps TransactionReminderWorker's package: WorkManager stores scheduled work by class name.

/** Schedules the daily evening reminder to add today's transactions, if the user wants it. */
class TransactionReminderScheduler @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
) {
    suspend fun scheduleReminder() {
        if (!appPreferences.showNotifications()) {
            return
        }

        val now = timeProvider.localNow()
        val today8PM = now.toLocalDate().atTime(ReminderTime)
        // start today if 8 PM is still ahead, otherwise tomorrow
        val nextReminder = if (today8PM.isAfter(now)) today8PM else today8PM.plusDays(1)
        val initialDelaySeconds = Duration.between(now, nextReminder).seconds

        val workBuilder = PeriodicWorkRequestBuilder<TransactionReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)

        WorkManager
            .getInstance(appContext)
            .enqueueUniquePeriodicWork(
                UniqueWorkName,
                ExistingPeriodicWorkPolicy.KEEP,
                workBuilder.build()
            )
    }

    private companion object {
        const val UniqueWorkName = "transaction_reminder_work_v2"
        val ReminderTime: LocalTime = LocalTime.of(20, 0)
    }
}
