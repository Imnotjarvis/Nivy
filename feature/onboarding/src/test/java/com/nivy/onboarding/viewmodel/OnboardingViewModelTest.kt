package com.nivy.onboarding.viewmodel

import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.AppSettings
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val appPreferences = mockk<AppPreferences>(relaxed = true) {
        // A new install: nothing saved yet.
        coEvery { settings() } returns null
    }

    private val viewModel = OnboardingViewModel(
        nav = mockk(relaxed = true),
        accountDao = mockk(relaxed = true),
        appPreferences = appPreferences,
        accountBalanceUseCase = mockk(relaxed = true),
        categoryCreator = mockk(relaxed = true),
        categoryRepository = mockk(relaxed = true),
        accountCreator = mockk(relaxed = true),
        accountRepository = mockk(relaxed = true),
        syncExchangeRatesUseCase = mockk(relaxed = true),
        transactionReminderScheduler = mockk(relaxed = true),
        starterData = mockk(relaxed = true),
        deleteAllDataUseCase = mockk(relaxed = true),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a new install has no savings goal until one is set`() {
        viewModel.start(isSystemDarkMode = false)

        val settings = slot<AppSettings>()
        coVerify { appPreferences.saveSettings(capture(settings)) }
        settings.captured.bufferAmount shouldBe 0.0
    }
}
