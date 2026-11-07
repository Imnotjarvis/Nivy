package com.ivy.wallet.domain.logic.notification

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.preferences.AppPreferences
import com.nivy.domain.AppStarter
import com.nivy.wallet.utils.atEndOfDay
import com.nivy.ui.R
import com.nivy.wallet.android.notification.NivyNotificationChannel
import com.nivy.wallet.android.notification.NotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Stays in com.ivy.wallet.domain.logic.notification: WorkManager stores this class name; a new one breaks the reminder.
@HiltWorker
class TransactionReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val notificationService: NotificationService,
    private val appPreferences: AppPreferences,
    private val appStarter: AppStarter,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val MINIMUM_TRANSACTIONS_PER_DAY = 1
    }

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val transactionsToday = with(timeConverter) {
            transactionDao.findAllBetween(
                startDate = timeProvider.localDateNow().atStartOfDay().toUTC(),
                endDate = timeProvider.localDateNow().atEndOfDay().toUTC(),
            )
        }

        val showNotifications = fetchShowNotifications()

        // Checked again here: the user may have turned notifications off in Settings after
        // tomorrow's reminder was scheduled.
        if (transactionsToday.size < MINIMUM_TRANSACTIONS_PER_DAY && showNotifications) {
            // Have less than 1 two transactions today, remind them

            val notification = notificationService
                .defaultNivyNotification(
                    channel = NivyNotificationChannel.TRANSACTION_REMINDER,
                    priority = NotificationCompat.PRIORITY_HIGH
                )
                .setContentTitle(applicationContext.applicationInfo.loadLabel(applicationContext.packageManager))
                .setContentText(randomText())
                .setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        1,
                        appStarter.getRootIntent(),
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_UPDATE_CURRENT
                                or PendingIntent.FLAG_IMMUTABLE
                    )
                )

            notificationService.showNotification(notification, 1)
        }

        return@withContext Result.success()
    }

    private fun randomText(): String =
        listOf(
            applicationContext.getString(R.string.notification_1),
            applicationContext.getString(R.string.notification_2),
            applicationContext.getString(R.string.notification_3),
        ).shuffled().first()

    private suspend fun fetchShowNotifications(): Boolean = appPreferences.showNotifications()
}
