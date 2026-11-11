package com.nivy.onboarding.viewmodel

import androidx.compose.runtime.mutableStateOf
import arrow.core.right
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.preferences.AppPreferences
import com.nivy.domain.usecase.data.DeleteAllDataUseCase
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.navigation.MainScreen
import com.nivy.navigation.Navigation
import com.nivy.navigation.OnboardingScreen
import com.nivy.onboarding.OnboardingState
import com.nivy.onboarding.StarterData
import com.nivy.wallet.domain.data.NivyCurrency
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingRouterTest {
    private val state = mutableStateOf(OnboardingState.SPLASH)
    private val nav = mockk<Navigation>(relaxed = true)
    private val accountDao = mockk<AccountDao> {
        coEvery { findAll() } returns emptyList()
    }
    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private val starterData = mockk<StarterData>(relaxed = true) {
        every { accountSuggestions(any()) } returns persistentListOf()
        every { categorySuggestions() } returns persistentListOf()
    }
    private val deleteAllData = mockk<DeleteAllDataUseCase>(relaxed = true)
    private val syncExchangeRates = mockk<SyncExchangeRatesUseCase> {
        coEvery { sync(any()) } returns Unit.right()
    }

    private val router = OnboardingRouter(
        state = state,
        accounts = mutableStateOf(persistentListOf()),
        accountSuggestions = mutableStateOf(persistentListOf()),
        categories = mutableStateOf(persistentListOf()),
        categorySuggestions = mutableStateOf(persistentListOf()),
        nav = nav,
        accountDao = accountDao,
        appPreferences = appPreferences,
        transactionReminderScheduler = mockk(relaxed = true),
        starterData = starterData,
        categoryRepository = mockk(relaxed = true),
        deleteAllDataUseCase = deleteAllData,
        syncExchangeRatesUseCase = syncExchangeRates,
    )

    @Test
    fun `the splash gives way to the welcome, and the welcome to the choice of a path`() = runTest {
        router.splashNext()
        state.value shouldBe OnboardingState.WELCOME

        router.welcomeNext()
        state.value shouldBe OnboardingState.CHOOSE_PATH
    }

    @Test
    fun `starting fresh and skipping the import both lead to the currency`() {
        state.value = OnboardingState.CHOOSE_PATH
        router.startFresh()
        state.value shouldBe OnboardingState.CURRENCY

        state.value = OnboardingState.CHOOSE_PATH
        router.importSkip()
        state.value shouldBe OnboardingState.CURRENCY
    }

    @Test
    fun `back goes one step back, and nowhere from the splash and the welcome`() = runTest {
        mapOf(
            OnboardingState.CATEGORIES to OnboardingState.ACCOUNTS,
            OnboardingState.ACCOUNTS to OnboardingState.CURRENCY,
            OnboardingState.CURRENCY to OnboardingState.CHOOSE_PATH,
            OnboardingState.CHOOSE_PATH to OnboardingState.WELCOME,
            OnboardingState.WELCOME to OnboardingState.WELCOME,
            OnboardingState.SPLASH to OnboardingState.SPLASH,
        ).forEach { (from, to) ->
            state.value = from
            router.onBack(viewModelScope = this, restartOnboarding = {})
            state.value shouldBe to
        }

        coVerify(exactly = 0) { deleteAllData() }
    }

    @Test
    fun `back from the currency of a restored wallet discards it and starts over`() = runTest {
        state.value = OnboardingState.CHOOSE_PATH
        router.importFinished(success = true)
        state.value shouldBe OnboardingState.CURRENCY
        var restarted = false

        router.onBack(viewModelScope = this, restartOnboarding = { restarted = true })
        advanceUntilIdle()

        coVerifyOrder {
            deleteAllData()
            nav.resetBackStack()
            nav.navigateTo(OnboardingScreen)
        }
        restarted shouldBe true
        state.value shouldBe OnboardingState.WELCOME

        // Nothing is restored any more: from the currency, back is one step again.
        router.startFresh()
        router.onBack(viewModelScope = this, restartOnboarding = {})
        state.value shouldBe OnboardingState.CHOOSE_PATH
        coVerify(exactly = 1) { deleteAllData() }
    }

    @Test
    fun `an import that didn't succeed keeps the choice of a path, and restores nothing`() = runTest {
        state.value = OnboardingState.CHOOSE_PATH

        router.importFinished(success = false)
        state.value shouldBe OnboardingState.CHOOSE_PATH

        // The wallet is a new one: its currency leads to the accounts, not out of onboarding.
        router.startFresh()
        router.setBaseCurrencyNext(Eur) { persistentListOf() }
        state.value shouldBe OnboardingState.ACCOUNTS
        coVerify(exactly = 0) { appPreferences.setOnboardingCompleted(any()) }
    }

    @Test
    fun `the currency of a restored wallet finishes onboarding, with its rates`() = runTest {
        state.value = OnboardingState.CHOOSE_PATH
        router.importFinished(success = true)

        router.setBaseCurrencyNext(Eur) { persistentListOf() }

        coVerifyOrder {
            appPreferences.setOnboardingCompleted(true)
            nav.resetBackStack()
            nav.navigateTo(MainScreen)
            syncExchangeRates.sync(AssetCode.unsafe("EUR"))
        }
        // Ready for a next time, after all the data is deleted.
        state.value shouldBe OnboardingState.SPLASH
    }

    @Test
    fun `the accounts lead to the categories`() = runTest {
        state.value = OnboardingState.ACCOUNTS

        router.accountsNext()

        state.value shouldBe OnboardingState.CATEGORIES
        coVerify(exactly = 0) { starterData.preloadAccounts() }
    }

    @Test
    fun `skipping the accounts makes the starter ones, and not a second time after going back`() = runTest {
        state.value = OnboardingState.ACCOUNTS
        router.accountsSkip()
        state.value shouldBe OnboardingState.CATEGORIES

        coEvery { accountDao.findAll() } returns listOf(mockk())
        router.onBack(viewModelScope = this, restartOnboarding = {})
        router.accountsSkip()

        coVerify(exactly = 1) { starterData.preloadAccounts() }
    }

    @Test
    fun `finishing the categories completes onboarding, and adds no starter category`() = runTest {
        state.value = OnboardingState.CATEGORIES

        router.categoriesNext(baseCurrency = Eur)

        coVerify(exactly = 1) { appPreferences.setOnboardingCompleted(true) }
        verify(exactly = 1) { nav.navigateTo(MainScreen) }
        coVerify(exactly = 0) { starterData.preloadCategories() }
    }

    @Test
    fun `skipping the categories makes the starter ones before onboarding is done and the rates download`() =
        runTest {
            state.value = OnboardingState.CATEGORIES

            router.categoriesSkip(baseCurrency = null)

            coVerifyOrder {
                starterData.preloadCategories()
                appPreferences.setOnboardingCompleted(true)
                syncExchangeRates.sync(any())
            }
        }

    private companion object {
        val Eur = NivyCurrency(code = "EUR", name = "Euro", isCrypto = false)
    }
}
