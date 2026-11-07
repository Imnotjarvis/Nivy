package com.ivy.wallet

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.RoundedCorner
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nivy.NivyNavGraph
import com.nivy.base.model.Theme
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.design.system.NivyMaterial3Theme
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.domain.RootScreen
import com.nivy.wallet.utils.activityForResultLauncher
import com.nivy.wallet.utils.simpleActivityForResultLauncher
import com.nivy.navigation.Navigation
import com.nivy.navigation.NavigationRoot
import com.nivy.ui.R
import com.nivy.ui.time.TimeFormatter
import com.nivy.ui.time.impl.DateTimePicker
import com.nivy.wallet.BuildConfig
import com.nivy.wallet.MainScreenState
import com.nivy.wallet.NivyTheme
import com.nivy.wallet.PeriodState
import com.nivy.wallet.RootViewModel
import com.nivy.wallet.ThemeState
import com.nivy.wallet.isDarkTheme
import com.nivy.wallet.ui.applocked.AppLockedScreen
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import com.ivy.widget.transaction.AddTransactionWidget
import com.ivy.widget.transaction.AddTransactionWidgetCompact
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.nivy.data.preferences.AppPreferences
import com.nivy.navigation.MainScreen
import kotlinx.coroutines.launch

// Stays in com.ivy.wallet: launchers and pinned shortcuts store this class name; a new one would drop them on update.
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

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var createFileLauncher: ActivityResultLauncher<String>
    private lateinit var openFileLauncher: ActivityResultLauncher<Unit>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private var notificationPermissionAsked = false

    private val viewModel: RootViewModel by viewModels()

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The icon stays until the first screen is chosen, which reads the settings first: until
        // then the window had nothing to draw.
        splashScreen.setKeepOnScreenCondition { navigation.currentScreen == null }
        setupApp()
        setupBackHandling()
        setContent {
            val viewModel: RootViewModel = viewModel()
            val isSystemInDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(isSystemInDarkTheme) {
                viewModel.start(isSystemInDarkTheme, intent)
            }
            LaunchedEffect(navigation.currentScreen) {
                if (navigation.currentScreen == MainScreen) askForNotificationPermission()
            }

            val appLocked by viewModel.appLocked.collectAsState()
            when (appLocked) {
                null -> { // display nothing
                }
                true -> {
                    NivyTheme(
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
                        NivyTheme(
                            theme = themeState.theme,
                            periodState = periodState,
                            mainScreenState = mainScreenState,
                            includeSurface = screen?.isLegacy ?: true,
                            timeConverter = timeConverter,
                            timeProvider = timeProvider,
                            timeFormatter = timeFormatter,
                        ) {
                            NivyNavGraph(screen)
                        }
                    }
                }
            }

            NivyMaterial3Theme(
                dark = isDarkTheme(themeState.theme, isSystemInDarkTheme),
                isTrueBlack = themeState.theme == Theme.AMOLED_DARK
            ) {
                dateTimePicker.Content()
            }
        }
    }

    private fun setupApp() {
        matchScreenCorners()
        setupActivityForResultLaunchers()
        AddTransactionWidget.updateBroadcast(this)
        AddTransactionWidgetCompact.updateBroadcast(this)
        WalletBalanceWidgetReceiver.updateBroadcast(this)
    }

    // By default blocks follow the screen's rounded corners: their radius less the margin blocks keep
    // from the edge. Android reports the radius from version 12.
    private fun matchScreenCorners() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val corner = display?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT) ?: return
        val radius = (corner.radius / resources.displayMetrics.density).dp
        AppCornerRadius.matchPhone = (radius - AppCornerRadius.ScreenMargin).coerceAtLeast(0.dp)
    }

    // Dismissing the dialog picks nothing.
    override fun pickDate(
        initialDate: LocalDate?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        onPicked: (LocalDate) -> Unit,
    ) {
        dateTimePicker.pickDate(
            initialDate = initialDate,
            minDate = minDate,
            maxDate = maxDate,
            onDatePick = onPicked,
        )
    }

    private fun setupActivityForResultLaunchers() {
        createFileLauncher()

        openFileLauncher()

        // Nothing to do with the answer: without the permission Android drops notifications.
        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    }

    /**
     * Android 13 and newer drop notifications until the user allows them, and nothing asked, so
     * the reminders never showed. Asks once, when the user is in the app with them switched on.
     */
    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationPermissionAsked) return
        notificationPermissionAsked = true
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return
        lifecycleScope.launch {
            if (appPreferences.showNotifications()) notificationPermissionLauncher.launch(permission)
        }
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
            intent?.data?.also(viewModel::filePicked)
        }
    }

    override fun createNewFile(fileName: String, onCreated: (Uri) -> Unit) {
        viewModel.awaitFile(onCreated)
        createFileLauncher.launch(fileName)
    }

    private fun openFileLauncher() {
        openFileLauncher = simpleActivityForResultLauncher(
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        ) { _, intent ->
            intent?.data?.also(viewModel::filePicked)
        }
    }

    override fun openFile(onOpened: (Uri) -> Unit) {
        viewModel.awaitFile(onOpened)
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

    @Suppress("TooGenericExceptionCaught")
    override fun openUrlInBrowser(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: Exception) {
            Timber.e(e)
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
