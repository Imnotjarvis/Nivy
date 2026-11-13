package com.nivy.wallet

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricPrompt
import com.nivy.base.model.AppLockDelay
import com.nivy.base.model.ColorPalette
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.preferences.AppPreferences
import com.nivy.design.system.colors.ColorMode
import com.nivy.domain.features.Features
import com.nivy.navigation.MainScreen
import com.nivy.navigation.Navigation
import com.nivy.navigation.SettingsScreen
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

    // Unconfined: a launched coroutine starts at once, before the code that launched it goes on,
    // as one on Dispatchers.IO can. Delays still run on the test's virtual clock.
    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatchersProvider {
        override val main: CoroutineContext = dispatcher
        override val io: CoroutineContext = dispatcher
        override val default: CoroutineContext = dispatcher
    }

    private val appLockSetting = MutableStateFlow(true)
    private val lockDelaySetting = MutableStateFlow(AppLockDelay.ONE_MINUTE)
    private val paletteSetting = MutableStateFlow(ColorPalette.NIVY)
    private val appPreferences = mockk<AppPreferences>(relaxed = true) {
        every { appLockEnabled } returns appLockSetting
        every { appLockDelay } returns lockDelaySetting
        every { dateFormat } returns emptyFlow()
        every { cornerRadius } returns emptyFlow()
        every { minimalColors } returns emptyFlow()
        every { colorPalette } returns paletteSetting
        coEvery { isAppLockEnabled() } answers { appLockSetting.value }
        coEvery { isOnboardingCompleted() } returns true
        coEvery { settings() } returns null
        coEvery { startDayOfMonth() } returns 1
    }
    private val nav = Navigation()

    private var now = 0L
    private val clock = mockk<ElapsedClock> { every { millis() } answers { now } }

    // A launch from the launcher: its extras hold no transaction type to add.
    private val intent = mockk<Intent>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ColorMode.palette = ColorPalette.NIVY
    }

    private fun viewModel() = RootViewModel(
        periodState = mockk(relaxed = true),
        themeState = mockk(relaxed = true),
        nav = nav,
        appPreferences = appPreferences,
        transactionReminderScheduler = mockk(relaxed = true),
        migrationsManager = mockk(relaxed = true),
        dateFormatState = mockk(relaxed = true),
        features = mockk<Features> { every { allFeatures } returns emptyList() },
        dispatchers = dispatchers,
        clock = clock,
        appContext = mockk<Context>(relaxed = true),
    )

    private fun startedViewModel() = viewModel().also { it.start(systemDarkMode = false, intent = intent) }

    /**
     * The app off the screen for [seconds], then back. Nothing of it runs meanwhile, as Android
     * freezes the apps it moves to the background: only the clock moves.
     */
    private fun RootViewModel.away(seconds: Int) {
        onAppHidden()
        now += seconds * 1_000L
        onAppShown()
    }

    @Test
    fun `the app's own colors follow the palette chosen in the settings`() = runTest(dispatcher) {
        // given
        startedViewModel()

        // when
        paletteSetting.value = ColorPalette.SUNSET

        // then
        ColorMode.palette shouldBe ColorPalette.SUNSET
    }

    @Test
    fun `until the setting is read, the app counts as locked`() = runTest(dispatcher) {
        // given
        appLockSetting.value = false

        // when
        val viewModel = viewModel()

        // then
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `with the lock on, the app starts locked`() = runTest(dispatcher) {
        // given
        appLockSetting.value = true

        // when
        val viewModel = startedViewModel()

        // then
        viewModel.appLocked.value shouldBe true
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `with the lock off, the app starts unlocked`() = runTest(dispatcher) {
        // given
        appLockSetting.value = false

        // when
        val viewModel = startedViewModel()

        // then
        viewModel.appLocked.value shouldBe false
        viewModel.isAppLocked() shouldBe false
    }

    @Test
    fun `the lock follows the setting while the app runs`() = runTest(dispatcher) {
        // given
        appLockSetting.value = false
        val viewModel = startedViewModel()

        // when
        appLockSetting.value = true
        runCurrent()

        // then
        viewModel.isAppLockEnabled() shouldBe true
    }

    @Test
    fun `a successful authentication unlocks the app`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        var authenticated = false
        val callback = viewModel.handleBiometricAuthResult(onAuthSuccess = { authenticated = true })

        // when
        callback.onAuthenticationSucceeded(mockk<BiometricPrompt.AuthenticationResult>())

        // then
        viewModel.isAppLocked() shouldBe false
        authenticated shouldBe true
    }

    @Test
    fun `a failed or cancelled authentication keeps the app locked`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        val callback = viewModel.handleBiometricAuthResult()

        // when
        callback.onAuthenticationFailed()
        callback.onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, "Cancelled")

        // then
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `a minute away locks the app, not a second earlier`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        viewModel.unlockApp()

        // when
        viewModel.away(seconds = 59)

        // then
        viewModel.isAppLocked() shouldBe false

        // when
        viewModel.away(seconds = 60)

        // then
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `every time away counts from zero`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        viewModel.unlockApp()

        // when
        viewModel.away(seconds = 50)
        viewModel.away(seconds = 50)

        // then
        viewModel.isAppLocked() shouldBe false
    }

    @Test
    fun `after a timed lock, the next time away gets a full minute again`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        viewModel.unlockApp()
        viewModel.away(seconds = 60)
        viewModel.unlockApp()

        // when
        viewModel.away(seconds = 59)

        // then
        viewModel.isAppLocked() shouldBe false
    }

    @Test
    fun `set to lock immediately, the app locks as it leaves the screen`() = runTest(dispatcher) {
        // given
        lockDelaySetting.value = AppLockDelay.IMMEDIATELY
        val viewModel = startedViewModel()
        viewModel.unlockApp()

        // when
        viewModel.onAppHidden()

        // then
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `set to five minutes, the app locks after five minutes away, not a second earlier`() = runTest(dispatcher) {
        // given
        lockDelaySetting.value = AppLockDelay.FIVE_MINUTES
        val viewModel = startedViewModel()
        viewModel.unlockApp()

        // when
        viewModel.away(seconds = 299)

        // then
        viewModel.isAppLocked() shouldBe false

        // when
        viewModel.away(seconds = 300)

        // then
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `the delay follows the setting while the app runs`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        viewModel.unlockApp()

        // when
        lockDelaySetting.value = AppLockDelay.FIVE_MINUTES
        viewModel.away(seconds = 120)

        // then
        viewModel.isAppLocked() shouldBe false
    }

    @Test
    fun `a file picker the app opened isn't leaving it, until the app is back`() = runTest(dispatcher) {
        // given
        lockDelaySetting.value = AppLockDelay.IMMEDIATELY
        val viewModel = startedViewModel()
        viewModel.unlockApp()

        // when
        viewModel.awaitFile {}
        viewModel.onAppHidden()

        // then
        viewModel.isAppLocked() shouldBe false

        // when
        viewModel.onAppShown()
        viewModel.onAppHidden()

        // then
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `time away while locked doesn't lock the app again once unlocked`() = runTest(dispatcher) {
        // given: locked, and its unlock shows the phone's PIN screen, which hides the app
        val viewModel = startedViewModel()
        viewModel.onAppHidden()
        now += 120_000

        // when
        viewModel.unlockApp()
        viewModel.onAppShown()

        // then
        viewModel.isAppLocked() shouldBe false
    }

    @Test
    fun `with the lock off, time away doesn't lock the app`() = runTest(dispatcher) {
        // given
        appLockSetting.value = false
        lockDelaySetting.value = AppLockDelay.IMMEDIATELY
        val viewModel = startedViewModel()

        // when
        viewModel.away(seconds = 600)

        // then
        viewModel.isAppLocked() shouldBe false
    }

    @Test
    fun `a recreated activity leaves the app unlocked and on its screen`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        nav.currentScreen shouldBe MainScreen
        viewModel.unlockApp()
        nav.navigateTo(SettingsScreen)

        // when
        viewModel.start(systemDarkMode = true, intent = intent)

        // then
        viewModel.isAppLocked() shouldBe false
        nav.currentScreen shouldBe SettingsScreen
    }
}
