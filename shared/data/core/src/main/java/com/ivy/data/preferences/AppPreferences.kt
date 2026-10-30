package com.ivy.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's settings and small bits of UI state, kept in DataStore.
 *
 * The key names are the ones the app used in its old SharedPreferences file ("ivy_wallet_prefs"):
 * the DataStore copies that file over on first use (see Datastore.kt), so users keep their
 * settings, and backups keep their format.
 */
@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun isOnboardingCompleted(): Boolean = get(OnboardingCompleted, false)
    suspend fun setOnboardingCompleted(completed: Boolean) = set(OnboardingCompleted, completed)

    suspend fun lastSelectedAccountId(): String? = dataStore.data.first()[LastSelectedAccountId]
    suspend fun setLastSelectedAccountId(id: String) = set(LastSelectedAccountId, id)

    suspend fun isAppLockEnabled(): Boolean = get(AppLockEnabled, false)
    suspend fun setAppLockEnabled(enabled: Boolean) = set(AppLockEnabled, enabled)

    suspend fun startDayOfMonth(): Int = get(StartDayOfMonth, 1)
    suspend fun setStartDayOfMonth(day: Int) = set(StartDayOfMonth, day)

    suspend fun showNotifications(): Boolean = get(ShowNotifications, true)
    suspend fun setShowNotifications(show: Boolean) = set(ShowNotifications, show)

    suspend fun hideCurrentBalance(): Boolean = get(HideCurrentBalance, false)
    suspend fun setHideCurrentBalance(hide: Boolean) = set(HideCurrentBalance, hide)

    suspend fun hideIncome(): Boolean = get(HideIncome, false)
    suspend fun setHideIncome(hide: Boolean) = set(HideIncome, hide)

    suspend fun transfersAsIncomeExpense(): Boolean = get(TransfersAsIncomeExpense, false)
    suspend fun setTransfersAsIncomeExpense(enabled: Boolean) = set(TransfersAsIncomeExpense, enabled)

    suspend fun categorySortOrder(default: Int): Int = get(CategorySortOrder, default)
    suspend fun setCategorySortOrder(order: Int) = set(CategorySortOrder, order)

    suspend fun isDataBackupCompleted(): Boolean = get(DataBackupCompleted, false)
    suspend fun setDataBackupCompleted(completed: Boolean) = set(DataBackupCompleted, completed)

    // Home screen cards the user dismissed, by card id.
    suspend fun isCardDismissed(cardId: String): Boolean = get(cardDismissedKey(cardId), false)
    suspend fun setCardDismissed(cardId: String) = set(cardDismissedKey(cardId), true)

    private suspend fun <T> get(key: Preferences.Key<T>, default: T): T =
        dataStore.data.first()[key] ?: default

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    companion object {
        /** The SharedPreferences file these settings used to live in. */
        const val LegacySharedPreferencesName = "ivy_wallet_prefs"

        private val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        private val LastSelectedAccountId = stringPreferencesKey("last_selected_account_id")
        private val AppLockEnabled = booleanPreferencesKey("lock_app")
        private val StartDayOfMonth = intPreferencesKey("start_date_of_month")
        private val ShowNotifications = booleanPreferencesKey("show_notifications")
        private val HideCurrentBalance = booleanPreferencesKey("hide_current_balance")
        private val HideIncome = booleanPreferencesKey("hide_income")
        private val TransfersAsIncomeExpense = booleanPreferencesKey("transfers_as_inc_exp")
        private val CategorySortOrder = intPreferencesKey("categorySortOrder")
        private val DataBackupCompleted = booleanPreferencesKey("data_backup_completed")
        private const val CardDismissedSuffix = "_cj_dismissed"

        private fun cardDismissedKey(cardId: String) = booleanPreferencesKey(cardId + CardDismissedSuffix)

        // The names backups use for the settings they carry.
        const val BackupShowNotifications = "show_notifications"
        const val BackupAppLockEnabled = "lock_app"
        const val BackupHideCurrentBalance = "hide_current_balance"
        const val BackupTransfersAsIncomeExpense = "transfers_as_inc_exp"
    }
}
