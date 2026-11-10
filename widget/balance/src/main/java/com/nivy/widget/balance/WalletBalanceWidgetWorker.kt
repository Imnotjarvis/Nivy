package com.nivy.widget.balance

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nivy.domain.AppStarter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Reads the balance widget's numbers again and redraws each widget placed. In WorkManager, the
 * work outlives the broadcast that asked for it: a coroutine of the receiver's own could be cut
 * short once the receiver returned.
 */
@HiltWorker
class WalletBalanceWidgetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val numbers: WalletBalanceWidgetNumbers,
    private val appStarter: AppStarter,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val shown = numbers.load()
        val widget = WalletBalanceWidget(getAppStarter = { appStarter })
        GlanceAppWidgetManager(applicationContext).getGlanceIds(WalletBalanceWidget::class.java).forEach { id ->
            updateAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[booleanPreferencesKey(PrefsKey.APP_LOCKED)] = shown.appLocked
                    this[doublePreferencesKey(PrefsKey.BALANCE)] = shown.balance
                    this[stringPreferencesKey(PrefsKey.CURRENCY)] = shown.currency
                    this[doublePreferencesKey(PrefsKey.INCOME)] = shown.income
                    this[doublePreferencesKey(PrefsKey.EXPENSE)] = shown.expense
                }
            }
            widget.update(applicationContext, id)
        }
        return Result.success()
    }

    companion object {
        private const val UniqueName = "wallet_balance_widget"

        /** Reads the numbers again and redraws; a request made meanwhile replaces one still to run. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UniqueName,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WalletBalanceWidgetWorker>().build(),
            )
        }
    }
}
