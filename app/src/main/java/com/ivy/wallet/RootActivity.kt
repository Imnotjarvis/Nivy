package com.ivy.wallet

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.ivy.IvyNavGraph
import com.ivy.base.model.Theme
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.domain.RootScreen
import com.ivy.legacy.MainScreenState
import com.ivy.legacy.PeriodState
import com.ivy.legacy.ThemeState
import com.ivy.legacy.IvyWalletTheme
import com.ivy.legacy.isDarkTheme
import com.ivy.legacy.utils.activityForResultLauncher
import com.ivy.legacy.utils.simpleActivityForResultLauncher
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.ui.R
import com.ivy.ui.time.TimeFormatter
import com.ivy.ui.time.impl.DateTimePicker
import com.ivy.wallet.ui.applocked.AppLockedScreen
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import com.ivy.widget.transaction.AddTransactionWidget
import com.ivy.widget.transaction.AddTransactionWidgetCompact
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class RootActivity : AppCompatActivity(), RootScreen {
    @Inject
    lateinit var themeState: ThemeState

    @Inject
    lateinit var periodState: PeriodState

    @Inject
    lateinit var mainScreenState: MainScreenState

    @Inject
    lateinit var navigation: Navigation

    @Inject
    lateinit var timeConverter: TimeConverter

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var timeFormatter: TimeFormatter

    @Inject
    lateinit var dateTimePicker: DateTimePicker

    private lateinit var createFileLauncher: ActivityResultLauncher<String>
    private lateinit var onFileCreated: (fileUri: Uri) -> Unit

    private lateinit var openFileLauncher: ActivityResultLauncher<Unit>
    private lateinit var onFileOpened: (fileUri: Uri) -> Unit

    private val viewModel: RootViewModel by viewModels()

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setupApp()
        setupBackHandling()
        setContent {
            val viewModel: RootViewModel = viewModel()
            val isSystemInDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(isSystemInDarkTheme) {
                viewModel.start(isSystemInDarkTheme, intent)
            }

            val appLocked by viewModel.appLocked.collectAsState()
            when (appLocked) {
                null -> { // display nothing
                }
                true -> {
                    IvyWalletTheme(
                        theme = themeState.theme,
                        periodState = periodState,
                        mainScreenState = mainScreenState,
                        timeConverter = timeConverter,
                        timeProvider = timeProvider,
                        timeFormatter = timeFormatter,
                    ) {
                        AppLockedScreen(
                            onShowOSBiometricsModal = {
                                authenticateWithOSBiometricsModal(
                                    biometricPromptCallback = viewModel.handleBiometricAuthResult()
                                )
                            },
                            onContinueWithoutAuthentication = {
                                viewModel.unlockApp()
                            }
                        )
                    }
                }

                false -> {
                    NavigationRoot(navigation = navigation) { screen ->
                        IvyWalletTheme(
                            theme = themeState.theme,
                            periodState = periodState,
                            mainScreenState = mainScreenState,
                            includeSurface = screen?.isLegacy ?: true,
                            timeConverter = timeConverter,
                            timeProvider = timeProvider,
                            timeFormatter = timeFormatter,
                        ) {
                            IvyNavGraph(screen)
                        }
                    }
                }
            }

            IvyMaterial3Theme(
                dark = isDarkTheme(themeState.theme, isSystemInDarkTheme),
                isTrueBlack = themeState.theme == Theme.AMOLED_DARK
            ) {
                dateTimePicker.Content()
            }
        }
    }

    private fun setupApp() {
        setupActivityForResultLaunchers()
        AddTransactionWidget.updateBroadcast(this)
        AddTransactionWidgetCompact.updateBroadcast(this)
        WalletBalanceWidgetReceiver.updateBroadcast(this)
    }

    private companion object {
        private const val MILLISECONDS_IN_DAY = 24 * 60 * 60 * 1000
    }

    override fun pickDate(
        initialDate: LocalDate?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        onPicked: (LocalDate) -> Unit,
    ) {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setSelection(
                    if (initialDate != null) {
                        initialDate.toEpochDay() * MILLISECONDS_IN_DAY
                    } else {
                        MaterialDatePicker.todayInUtcMilliseconds()
                    }
                )
                .build()
        datePicker.show(supportFragmentManager, "datePicker")
        datePicker.addOnPositiveButtonClickListener {
            onPicked(LocalDate.ofEpochDay(it / MILLISECONDS_IN_DAY))
        }

        if (minDate != null) {
            datePicker.addOnCancelListener {
                onPicked(minDate)
            }
        }

        if (maxDate != null) {
            datePicker.addOnCancelListener {
                onPicked(maxDate)
            }
        }

        if (initialDate != null) {
            datePicker.addOnCancelListener {
                onPicked(initialDate)
            }
        }
    }

    override fun pickTime(initialTime: LocalTime?, onPicked: (LocalTime) -> Unit) {
        val nowLocal = initialTime ?: timeProvider.localTimeNow()
        val is24Hour = android.text.format.DateFormat.is24HourFormat(this)
        val timeFormat = if (is24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(timeFormat)
                .setHour(nowLocal.hour)
                .setMinute(nowLocal.minute)
                .build()
        picker.show(supportFragmentManager, "timePicker")
        picker.addOnPositiveButtonClickListener {
            onPicked(
                LocalTime.of(picker.hour, picker.minute).withSecond(0)
            )
        }
    }

    private fun setupActivityForResultLaunchers() {
        createFileLauncher()

        openFileLauncher()
    }

    private fun createFileLauncher() {
        createFileLauncher = activityForResultLauncher(
            createIntent = { _, fileName ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/csv"
                    putExtra(Intent.EXTRA_TITLE, fileName)

                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker before your app creates the document.
                    putExtra(
                        DocumentsContract.EXTRA_INITIAL_URI,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .toURI()
                    )
                }
            }
        ) { _, intent ->
            intent?.data?.also {
                onFileCreated(it)
            }
        }
    }

    override fun createNewFile(fileName: String, onCreated: (Uri) -> Unit) {
        onFileCreated = onCreated
        createFileLauncher.launch(fileName)
    }

    private fun openFileLauncher() {
        openFileLauncher = simpleActivityForResultLauncher(
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        ) { _, intent ->
            intent?.data?.also {
                onFileOpened(it)
            }
        }
    }

    override fun openFile(onOpened: (Uri) -> Unit) {
        onFileOpened = onOpened
        openFileLauncher.launch(Unit)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (viewModel.isAppLockEnabled() && !hasFocus) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isAppLockEnabled()) {
            viewModel.checkUserInactiveTimeStatus()
        }
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.isAppLockEnabled()) {
            viewModel.startUserInactiveTimeCounter()
        }
    }

    private fun authenticateWithOSBiometricsModal(
        biometricPromptCallback: BiometricPrompt.AuthenticationCallback
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            biometricPromptCallback
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(
                getString(R.string.authentication_required)
            )
            .setSubtitle(
                getString(R.string.authentication_required_description)
            )
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .setConfirmationRequired(false)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Screens and dialogs handle back with Compose's BackHandler, which takes precedence over this
    // callback. What they don't consume goes back a screen; at the first screen, or while the app
    // is locked, it falls through to the default behaviour (leaving the app).
    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.isAppLocked() || !navigation.back()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "PrintStackTrace")
    override fun openUrlInBrowser(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.data = Uri.parse(url)
            startActivity(browserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "No browser app found. Visit manually: $url",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun shareCSVFile(fileUri: Uri) {
        val intent = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/csv"
            },
            null
        )
        startActivity(intent)
    }

    override fun shareZipFile(fileUri: Uri) {
        val intent = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "application/zip"
            },
            null
        )
        startActivity(intent)
    }

    override val buildVersionName: String
        get() = BuildConfig.VERSION_NAME
    override val buildVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    override fun <T> pinWidget(widget: Class<T>) {
        val appWidgetManager: AppWidgetManager = this.getSystemService(AppWidgetManager::class.java)
        val addTransactionWidget = ComponentName(this, widget)
        appWidgetManager.requestPinAppWidget(addTransactionWidget, null, null)
    }
}
