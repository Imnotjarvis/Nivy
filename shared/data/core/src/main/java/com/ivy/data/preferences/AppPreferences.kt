package com.ivy.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ivy.base.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    /** The lock setting as it changes, for the activity that enforces it. */
    val appLockEnabled: Flow<Boolean> =
        dataStore.data.map { it[AppLockEnabled] ?: false }.distinctUntilChanged()

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

    /** The user's settings; null until onboarding (or a backup, or the old database) sets them. */
    suspend fun settings(): AppSettings? {
        val prefs = dataStore.data.first()
        val baseCurrency = prefs[BaseCurrency] ?: return null
        return AppSettings(
            theme = prefs[ThemeName]?.let { name -> Theme.entries.find { it.name == name } } ?: Theme.AUTO,
            baseCurrency = baseCurrency,
            bufferAmount = prefs[BufferAmount] ?: 0.0,
            name = prefs[UserName].orEmpty(),
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit {
            it[ThemeName] = settings.theme.name
            it[BaseCurrency] = settings.baseCurrency
            it[BufferAmount] = settings.bufferAmount
            it[UserName] = settings.name
        }
    }

    suspend fun baseCurrency(): String? = dataStore.data.first()[BaseCurrency]
    suspend fun setBaseCurrency(code: String) = set(BaseCurrency, code)
    suspend fun setTheme(theme: Theme) = set(ThemeName, theme.name)
    suspend fun setBufferAmount(amount: Double) = set(BufferAmount, amount)
    suspend fun setUserName(name: String) = set(UserName, name)

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
        private const val CardDismissedSuffix = "_cj_dismissed"

        // The settings that used to live in the database's settings table.
        internal val ThemeName = stringPreferencesKey("settings_theme")
        internal val BaseCurrency = stringPreferencesKey("settings_base_currency")
        internal val BufferAmount = doublePreferencesKey("settings_buffer_amount")
        internal val UserName = stringPreferencesKey("settings_name")

        private fun cardDismissedKey(cardId: String) = booleanPreferencesKey(cardId + CardDismissedSuffix)

        // The names backups use for the settings they carry.
        const val BackupShowNotifications = "show_notifications"
        const val BackupAppLockEnabled = "lock_app"
        const val BackupHideCurrentBalance = "hide_current_balance"
        const val BackupTransfersAsIncomeExpense = "transfers_as_inc_exp"
    }
}
