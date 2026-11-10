package com.nivy.onboarding.viewmodel

import androidx.compose.runtime.mutableStateOf
import arrow.core.right
import com.nivy.data.preferences.AppPreferences
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.onboarding.OnboardingState
import com.nivy.onboarding.StarterData
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OnboardingRouterTest {
    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private val starterData = mockk<StarterData>(relaxed = true)
    private val syncExchangeRates = mockk<SyncExchangeRatesUseCase> {
        coEvery { sync(any()) } returns Unit.right()
    }

    private val router = OnboardingRouter(
        state = mutableStateOf(OnboardingState.CATEGORIES),
        accounts = mutableStateOf(persistentListOf()),
        accountSuggestions = mutableStateOf(persistentListOf()),
        categories = mutableStateOf(persistentListOf()),
        categorySuggestions = mutableStateOf(persistentListOf()),
        nav = mockk(relaxed = true),
        accountDao = mockk(relaxed = true),
        appPreferences = appPreferences,
        transactionReminderScheduler = mockk(relaxed = true),
        starterData = starterData,
        categoryRepository = mockk(relaxed = true),
        deleteAllDataUseCase = mockk(relaxed = true),
        syncExchangeRatesUseCase = syncExchangeRates,
    )

    @Test
    fun `skipping the categories makes the starter ones before onboarding is done and the rates download`() =
        runTest {
            router.categoriesSkip(baseCurrency = null)

            coVerifyOrder {
                starterData.preloadCategories()
                appPreferences.setOnboardingCompleted(true)
                syncExchangeRates.sync(any())
            }
        }
}
