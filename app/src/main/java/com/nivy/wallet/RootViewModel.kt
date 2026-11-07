package com.nivy.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.biometric.BiometricPrompt
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.preferences.AppPreferences
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.design.system.colors.ColorMode
import com.nivy.domain.features.Features
import com.nivy.wallet.utils.ioThread
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
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
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    companion object {
        /** The action the launcher shortcuts and the widgets open the app with (AndroidManifest). */
        const val ACTION_ADD_TRANSACTION = "ivy.wallet.intent.action.add_transaction"
        const val EXTRA_ADD_TRANSACTION_TYPE = "add_transaction_type_extra"

        const val USER_INACTIVITY_TIME_LIMIT = 60 // Time in seconds
    }

    // Kept current by the collector in start(); read by the activity on the main thread.
    @Volatile
    private var appLockEnabled = false

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
    }

    fun filePicked(uri: Uri) {
        onFilePicked?.invoke(uri)
        onFilePicked = null
    }

    fun start(systemDarkMode: Boolean, intent: Intent) {
        viewModelScope.launch {
            ioThread {
                val theme = appPreferences.settings()?.theme
                    ?: if (systemDarkMode) Theme.DARK else Theme.LIGHT
                themeState.switchTheme(theme)

                appPreferences.startDayOfMonth().also(periodState::setStartDayOfMonth)
            }
        }

        if (started) return
        started = true

        viewModelScope.launch {
            // Settings can turn the lock on or off while the app runs; the activity asks
            // isAppLockEnabled() on every pause and focus change, so the answer has to follow.
            appPreferences.appLockEnabled.collect { appLockEnabled = it }
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
            ioThread {
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

    // App Lock & UserInactivity --------------------------------------------------------------------
    fun isAppLockEnabled(): Boolean {
        return appLockEnabled
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

    private val userInactiveTime = AtomicLong(0)
    private var userInactiveJob: Job? = null

    @Suppress("MagicNumber")
    fun startUserInactiveTimeCounter() {
        if (userInactiveJob != null && userInactiveJob!!.isActive) return
        // Counts from this pause: after a timed lock the count stayed at the limit, so every
        // later pause, a file picker included, locked the app at once.
        resetUserInactiveTimer()

        userInactiveJob = viewModelScope.launch(Dispatchers.IO) {
            while (userInactiveTime.get() < USER_INACTIVITY_TIME_LIMIT &&
                userInactiveJob != null && !userInactiveJob?.isCancelled!!
            ) {
                delay(1000)
                userInactiveTime.incrementAndGet()
            }

            if (!isAppLocked()) {
                lockApp()
            }

            cancel()
        }
    }

    fun checkUserInactiveTimeStatus() {
        if (userInactiveTime.get() < USER_INACTIVITY_TIME_LIMIT) {
            if (userInactiveJob != null && userInactiveJob?.isCancelled == false) {
                userInactiveJob?.cancel()
                resetUserInactiveTimer()
            }
        }
    }

    fun resetUserInactiveTimer() {
        userInactiveTime.set(0)
    }
}
