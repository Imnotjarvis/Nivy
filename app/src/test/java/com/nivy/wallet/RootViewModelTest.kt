package com.nivy.wallet

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricPrompt
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.preferences.AppPreferences
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

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
    private val appPreferences = mockk<AppPreferences>(relaxed = true) {
        every { appLockEnabled } returns appLockSetting
        every { dateFormat } returns emptyFlow()
        every { cornerRadius } returns emptyFlow()
        every { minimalColors } returns emptyFlow()
        coEvery { isAppLockEnabled() } answers { appLockSetting.value }
        coEvery { isOnboardingCompleted() } returns true
        coEvery { settings() } returns null
        coEvery { startDayOfMonth() } returns 1
    }
    private val nav = Navigation()

    // A launch from the launcher: its extras hold no transaction type to add.
    private val intent = mockk<Intent>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        appContext = mockk<Context>(relaxed = true),
    )

    private fun startedViewModel() = viewModel().also { it.start(systemDarkMode = false, intent = intent) }

    /** The app in the background for [seconds], on the virtual clock. */
    private fun TestScope.away(seconds: Int) {
        advanceTimeBy(seconds.seconds)
        runCurrent()
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
        viewModel.startUserInactiveTimeCounter()
        away(seconds = 59)

        // then
        viewModel.isAppLocked() shouldBe false
        away(seconds = 1)
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `coming back within the minute keeps the app unlocked`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        viewModel.unlockApp()
        viewModel.startUserInactiveTimeCounter()
        away(seconds = 30)

        // when
        viewModel.checkUserInactiveTimeStatus()
        away(seconds = 60)

        // then
        viewModel.isAppLocked() shouldBe false
    }

    @Test
    fun `every time away counts from zero`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        viewModel.unlockApp()
        viewModel.startUserInactiveTimeCounter()
        away(seconds = 50)
        viewModel.checkUserInactiveTimeStatus()

        // when
        viewModel.startUserInactiveTimeCounter()
        away(seconds = 59)

        // then
        viewModel.isAppLocked() shouldBe false
        away(seconds = 1)
        viewModel.isAppLocked() shouldBe true
    }

    @Test
    fun `after a timed lock, the next time away gets a full minute again`() = runTest(dispatcher) {
        // given
        val viewModel = startedViewModel()
        viewModel.unlockApp()
        viewModel.startUserInactiveTimeCounter()
        away(seconds = 60)
        viewModel.checkUserInactiveTimeStatus()
        viewModel.unlockApp()

        // when
        viewModel.startUserInactiveTimeCounter()
        away(seconds = 59)

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
