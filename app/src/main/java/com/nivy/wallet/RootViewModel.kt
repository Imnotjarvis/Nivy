package com.nivy.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.biometric.BiometricPrompt
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nivy.base.model.AppLockDelay
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.preferences.AppPreferences
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.design.system.colors.ColorMode
import com.nivy.domain.features.Features
import com.nivy.wallet.utils.readOnly
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.MainScreen
import com.nivy.navigation.Navigation
import com.nivy.navigation.OnboardingScreen
import com.nivy.ui.time.DateFormatState
import com.nivy.wallet.domain.logic.notification.TransactionReminderScheduler
import com.nivy.wallet.migrations.MigrationsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class RootViewModel @Inject constructor(
    private val periodState: PeriodState,
    private val themeState: ThemeState,
    private val nav: Navigation,
    private val appPreferences: AppPreferences,
    private val transactionReminderScheduler: TransactionReminderScheduler,
    private val migrationsManager: MigrationsManager,
    private val dateFormatState: DateFormatState,
    private val features: Features,
    private val dispatchers: DispatchersProvider,
    private val clock: ElapsedClock,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    companion object {
        /** The action the launcher shortcuts and the widgets open the app with (AndroidManifest). */
        const val ACTION_ADD_TRANSACTION = "ivy.wallet.intent.action.add_transaction"
        const val EXTRA_ADD_TRANSACTION_TYPE = "add_transaction_type_extra"
    }

    // Kept current by the collectors in start(); read by the activity on the main thread.
    private val lockOn = MutableStateFlow(false)

    /** Whether the lock is on, as the settings change it. */
    val appLockOn = lockOn.readOnly()

    @Volatile
    private var appLockDelay = AppLockDelay.Default

    // When the app left the screen unlocked, on the clock; null while it's on screen.
    private var leftAt: Long? = null

    // A file picker the app opened is on screen: going to it isn't leaving the app.
    private var pickingFile = false

    // The activity is recreated on configuration changes (dark mode, language, and from
    // Android 16 rotation on tablets, where the portrait lock no longer applies) and calls
    // start() again. This ViewModel outlives that, so the first navigation, which would
    // otherwise send the user back to the home or add-transaction screen, and the migrations
    // run once.
    private var started = false

    private val _appLocked = MutableStateFlow<Boolean?>(null)
    val appLocked = _appLocked.readOnly()

    // The export or import waiting for the system file picker. It lives here rather than in the
    // activity, which Android may recreate (a rotation on a tablet) while the picker is open.
    private var onFilePicked: ((Uri) -> Unit)? = null

    fun awaitFile(onPicked: (Uri) -> Unit) {
        onFilePicked = onPicked
        pickingFile = true
    }

    fun filePicked(uri: Uri) {
        onFilePicked?.invoke(uri)
        onFilePicked = null
    }

    fun start(systemDarkMode: Boolean, intent: Intent) {
        viewModelScope.launch {
            withContext(dispatchers.io) {
                val theme = appPreferences.settings()?.theme
                    ?: if (systemDarkMode) Theme.DARK else Theme.LIGHT
                themeState.switchTheme(theme)

                appPreferences.startDayOfMonth().also(periodState::setStartDayOfMonth)
            }
        }

        if (started) return
        started = true

        viewModelScope.launch {
            // Settings can turn the lock on or off while the app runs: the activity and the
            // time away follow it.
            appPreferences.appLockEnabled.collect { lockOn.value = it }
        }

        viewModelScope.launch {
            // How long the app can be away before it locks, as chosen in the settings.
            appPreferences.appLockDelay.collect { appLockDelay = it }
        }

        viewModelScope.launch {
            // Dates follow the format chosen in the settings, from start-up and as it changes.
            appPreferences.dateFormat.collect { dateFormatState.format = it }
        }

        viewModelScope.launch {
            // Blocks and pills follow the corner radius chosen in the settings.
            appPreferences.cornerRadius.collect { AppCornerRadius.setting = it?.dp }
        }

        viewModelScope.launch {
            // The app is drawn in minimal colors when the settings say so.
            appPreferences.minimalColors.collect { ColorMode.minimal = it }
        }

        viewModelScope.launch {
            withContext(dispatchers.io) {
                // initial app locked state
                _appLocked.value = appPreferences.isAppLockEnabled()

                // Read before the first screen, so screens start from the stored features, not
                // from their defaults: a compact list doesn't open as cards first.
                features.allFeatures.forEach { it.isEnabled(appContext) }

                // Navigation is a singleton that outlives activities: a widget's task-clearing
                // launch would otherwise put the old activity's screens behind the new one.
                nav.resetBackStack()
                if (isOnboardingCompleted()) {
                    navigateOnboardedUser(intent)
                } else {
                    nav.navigateTo(OnboardingScreen)
                }
            }
        }

        viewModelScope.launch {
            migrationsManager.executeMigrations()
        }
    }

    private suspend fun navigateOnboardedUser(intent: Intent) {
        if (!handleSpecialStart(intent)) {
            nav.navigateTo(MainScreen)
            transactionReminderScheduler.scheduleReminder()
        }
    }

    @Suppress("SwallowedException")
    private fun handleSpecialStart(intent: Intent): Boolean {
        val addTrnType: TransactionType? = try {
            IntentCompat.getSerializableExtra(
                intent,
                EXTRA_ADD_TRANSACTION_TYPE,
                TransactionType::class.java
            )
                ?: TransactionType.valueOf(intent.getStringExtra(EXTRA_ADD_TRANSACTION_TYPE) ?: "")
        } catch (e: IllegalArgumentException) {
            null
        }

        if (addTrnType != null) {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = addTrnType
                )
            )

            return true
        }

        return false
    }

    @Suppress("EmptyFunctionBlock")
    fun handleBiometricAuthResult(
        onAuthSuccess: () -> Unit = {}
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Timber.d("Authentication succeeded")
                unlockApp()
                onAuthSuccess()
            }

            override fun onAuthenticationFailed() {
                Timber.d("Authentication failed")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            }
        }
    }

    private suspend fun isOnboardingCompleted(): Boolean {
        return appPreferences.isOnboardingCompleted()
    }

    // App lock and time away ----------------------------------------------------------------------
    fun isAppLockEnabled(): Boolean {
        return lockOn.value
    }

    fun isAppLocked(): Boolean {
        // by default we assume that the app is locked
        return appLocked.value ?: true
    }

    fun lockApp() {
        _appLocked.value = true
    }

    fun unlockApp() {
        _appLocked.value = false
    }

    /**
     * The app left the screen: home, another app, or the screen turned off. With the lock on, it
     * locks at once if the settings say so; otherwise it notes when, for [onAppShown]. Going to a
     * file picker the app opened isn't leaving it, and a locked app has nothing to count: its
     * unlock can show the phone's own PIN screen.
     */
    fun onAppHidden() {
        if (!lockOn.value || isAppLocked() || pickingFile) return
        if (appLockDelay == AppLockDelay.IMMEDIATELY) {
            lockApp()
        } else {
            leftAt = clock.millis()
        }
    }

    /**
     * The app is back on screen: it locks if it was away at least as long as the settings allow.
     * The time away is measured on the clock, not counted while the app runs, as Android freezes
     * the apps it moves to the background.
     */
    fun onAppShown() {
        pickingFile = false
        val left = leftAt ?: return
        leftAt = null
        if (lockOn.value && clock.millis() - left >= appLockDelay.millis) lockApp()
    }
}
