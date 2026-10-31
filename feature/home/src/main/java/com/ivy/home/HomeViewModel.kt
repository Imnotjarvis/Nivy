package com.ivy.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.Theme
import com.ivy.base.model.next
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Transaction
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.preferences.AppSettings
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.features.Features
import com.ivy.domain.usecase.planned.DueTransactionsUseCase
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.home.customerjourney.CustomerJourneyCardsProvider
import com.ivy.wallet.MainScreenState
import com.ivy.wallet.PeriodState
import com.ivy.wallet.ThemeState
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.data.BufferInfo
import com.ivy.wallet.data.DueSection
import com.ivy.wallet.data.model.MainTab
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.data.model.toUTCCloseTimeRange
import com.ivy.wallet.domain.action.viewmodel.home.ShouldHideIncomeAct
import com.ivy.wallet.utils.ioThread
import com.ivy.navigation.BalanceScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.global.StartDayOfMonthAct
import com.ivy.wallet.domain.action.settings.CalcBufferDiffAct
import com.ivy.wallet.domain.action.transaction.HistoryWithDateDivsAct
import com.ivy.wallet.domain.action.viewmodel.home.HasTrnsAct
import com.ivy.wallet.domain.action.viewmodel.home.OverdueAct
import com.ivy.wallet.domain.action.viewmodel.home.ShouldHideBalanceAct
import com.ivy.wallet.domain.action.viewmodel.home.UpcomingAct
import com.ivy.wallet.domain.action.wallet.CalcIncomeExpenseAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@Stable
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val periodState: PeriodState,
    private val themeState: ThemeState,
    private val mainScreenState: MainScreenState,
    private val nav: Navigation,
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val customerJourneyLogic: CustomerJourneyCardsProvider,
    private val historyWithDateDivsAct: HistoryWithDateDivsAct,
    private val calcIncomeExpenseAct: CalcIncomeExpenseAct,
    private val calcWalletBalanceAct: CalcWalletBalanceAct,
    private val appPreferences: AppPreferences,
    private val currencyRepository: CurrencyRepository,
    private val accountsAct: AccountsAct,
    private val categoryRepository: CategoryRepository,
    private val calcBufferDiffAct: CalcBufferDiffAct,
    private val upcomingAct: UpcomingAct,
    private val overdueAct: OverdueAct,
    private val hasTrnsAct: HasTrnsAct,
    private val startDayOfMonthAct: StartDayOfMonthAct,
    private val shouldHideBalanceAct: ShouldHideBalanceAct,
    private val shouldHideIncomeAct: ShouldHideIncomeAct,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val features: Features
) : ComposeViewModel<HomeState, HomeEvent>() {
    private var currentTheme by mutableStateOf(Theme.AUTO)
    private var name by mutableStateOf("")
    private var period by mutableStateOf(periodState.selected)
    private var baseData by mutableStateOf(
        AppBaseData(
            baseCurrency = "",
            accounts = persistentListOf(),
            categories = persistentListOf()
        )
    )
    private var history by mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())
    private var stats by mutableStateOf(IncomeExpensePair.zero())
    private var balance by mutableStateOf(BigDecimal.ZERO)
    private var buffer by mutableStateOf(
        BufferInfo(
            amount = BigDecimal.ZERO,
            bufferDiff = BigDecimal.ZERO,
        )
    )
    private var upcoming by mutableStateOf(
        DueSection(
            trns = persistentListOf(),
            stats = IncomeExpensePair.zero(),
            expanded = false,
        )
    )
    private var overdue by mutableStateOf(
        DueSection(
            trns = persistentListOf(),
            stats = IncomeExpensePair.zero(),
            expanded = false,
        )
    )
    private var customerJourneyCards by
    mutableStateOf<ImmutableList<CustomerJourneyCardModel>>(persistentListOf())
    private var hideBalance by mutableStateOf(false)
    private var hideIncome by mutableStateOf(false)
    private var expanded by mutableStateOf(true)

    @Composable
    override fun uiState(): HomeState {
        LaunchedEffect(Unit) {
            start()
        }

        return HomeState(
            theme = getTheme(),
            name = getName(),
            period = getPeriod(),
            baseData = getBaseData(),
            history = getHistory(),
            stats = getStats(),
            balance = getBalance(),
            buffer = getBuffer(),
            upcoming = getUpcoming(),
            overdue = getOverdue(),
            customerJourneyCards = getCustomerJourneyCards(),
            hideBalance = getHideBalance(),
            expanded = getExpanded(),
            hideIncome = getHideIncome(),
            shouldShowAccountSpecificColorInTransactions = getShouldShowAccountSpecificColorInTransactions()
        )
    }

    @Composable
    fun getShouldShowAccountSpecificColorInTransactions(): Boolean {
        return features.showAccountColorsInTransactions.asEnabledState()
    }

    @Composable
    private fun getTheme(): Theme {
        return currentTheme
    }

    @Composable
    private fun getName(): String {
        return name
    }

    @Composable
    private fun getPeriod(): TimePeriod {
        return period
    }

    @Composable
    private fun getBaseData(): AppBaseData {
        return baseData
    }

    @Composable
    private fun getHistory(): ImmutableList<TransactionHistoryItem> {
        return history
    }

    @Composable
    private fun getStats(): IncomeExpensePair {
        return stats
    }

    @Composable
    private fun getBalance(): BigDecimal {
        return balance
    }

    @Composable
    private fun getBuffer(): BufferInfo {
        return buffer
    }

    @Composable
    private fun getUpcoming(): DueSection {
        return upcoming
    }

    @Composable
    private fun getOverdue(): DueSection {
        return overdue
    }

    @Composable
    private fun getCustomerJourneyCards(): ImmutableList<CustomerJourneyCardModel> {
        return customerJourneyCards
    }

    @Composable
    private fun getHideBalance(): Boolean {
        return hideBalance
    }

    @Composable
    private fun getExpanded(): Boolean {
        return expanded
    }

    @Composable
    private fun getHideIncome(): Boolean {
        return hideIncome
    }

    override fun onEvent(event: HomeEvent) {
        viewModelScope.launch {
            when (event) {
                HomeEvent.BalanceClick -> onBalanceClick()
                HomeEvent.HiddenBalanceClick -> onHiddenBalanceClick()
                HomeEvent.HiddenIncomeClick -> onHiddenIncomeClick()
                is HomeEvent.PayOrGetPlanned -> payOrGetPlanned(event.transaction)
                is HomeEvent.SkipPlanned -> skipPlanned(event.transaction)
                is HomeEvent.SkipAllPlanned -> skipAllPlanned(event.transactions)
                is HomeEvent.SetPeriod -> setPeriod(event.period)
                HomeEvent.SelectNextMonth -> onSelectNextMonth()
                HomeEvent.SelectPreviousMonth -> onSelectPreviousMonth()
                is HomeEvent.SetUpcomingExpanded -> setUpcomingExpanded(event.expanded)
                is HomeEvent.SetOverdueExpanded -> setOverdueExpanded(event.expanded)
                is HomeEvent.SetBuffer -> setBuffer(event.buffer)
                HomeEvent.SwitchTheme -> switchTheme()
                is HomeEvent.DismissCustomerJourneyCard -> dismissCustomerJourneyCard(event.card)
                is HomeEvent.SetExpanded -> setExpanded(event.expanded)
            }
        }
    }

    private suspend fun start() {
        val startDay = startDayOfMonthAct()
        reload(periodState.initSelected(startDayOfMonth = startDay))
    }

    // -----------------------------------------------------------------------------------
    private suspend fun reload(
        timePeriod: TimePeriod = periodState.selected
    ) {
        val stored = appPreferences.settings()
        val settings = AppSettings(
            theme = stored?.theme ?: themeState.theme,
            baseCurrency = currencyRepository.getBaseCurrency().code,
            bufferAmount = stored?.bufferAmount ?: 0.0,
            name = stored?.name.orEmpty(),
        )
        val hideBalance = shouldHideBalanceAct()
        val hideIncome = shouldHideIncomeAct()

        currentTheme = settings.theme
        name = settings.name
        period = timePeriod
        this.hideBalance = hideBalance
        this.hideIncome = hideIncome

        // This method is used to restore the theme when user imports locally backed up data
        themeState.switchTheme(theme = settings.theme)

        val timeRange = period.toRange(
            startDateOfMonth = periodState.startDayOfMonth,
            timeConverter = timeConverter,
            timeProvider = timeProvider
        ).toUTCCloseTimeRange()

        val withAccounts = loadAppBaseData(settings to timeRange)
        val withBalance = loadIncomeExpenseBalance(withAccounts)
        val currencyAndRange = loadBuffer(withBalance)
        loadTrnHistory(currencyAndRange)
        loadDueTrns(currencyAndRange)
        loadCustomerJourney()
    }

    private suspend fun loadAppBaseData(
        input: Pair<AppSettings, ClosedTimeRange>
    ): Triple<AppSettings, ClosedTimeRange, List<Account>> {
        val (settings, timeRange) = input
        val accounts = accountsAct()
        val categories = categoryRepository.findAll()

        baseData = AppBaseData(
            baseCurrency = settings.baseCurrency,
            categories = categories.toImmutableList(),
            accounts = accounts.toImmutableList()
        )

        return Triple(settings, timeRange, accounts)
    }

    private suspend fun loadIncomeExpenseBalance(
        input: Triple<AppSettings, ClosedTimeRange, List<Account>>
    ): Triple<AppSettings, ClosedTimeRange, BigDecimal> {
        val (settings, timeRange, accounts) = input

        val incomeExpense = calcIncomeExpenseAct(
            CalcIncomeExpenseAct.Input(
                baseCurrency = settings.baseCurrency,
                accounts = accounts,
                range = timeRange
            )
        )

        val balanceAmount = calcWalletBalanceAct(
            CalcWalletBalanceAct.Input(baseCurrency = settings.baseCurrency)
        )

        balance = balanceAmount
        stats = incomeExpense

        return Triple(settings, timeRange, balanceAmount)
    }

    private suspend fun loadBuffer(
        input: Triple<AppSettings, ClosedTimeRange, BigDecimal>
    ): Pair<String, ClosedTimeRange> {
        val (settings, timeRange, balance) = input

        buffer = BufferInfo(
            amount = settings.bufferAmount.toBigDecimal(),
            bufferDiff = calcBufferDiffAct(
                CalcBufferDiffAct.Input(
                    balance = balance,
                    buffer = settings.bufferAmount.toBigDecimal()
                )
            )
        )

        return settings.baseCurrency to timeRange
    }

    private suspend fun loadTrnHistory(
        input: Pair<String, ClosedTimeRange>
    ): Pair<String, ClosedTimeRange> {
        val (baseCurrency, timeRange) = input

        history = historyWithDateDivsAct(
            HistoryWithDateDivsAct.Input(
                range = timeRange,
                baseCurrency = baseCurrency
            )
        )

        return baseCurrency to timeRange
    }

    private suspend fun loadDueTrns(input: Pair<String, ClosedTimeRange>) {
        val (baseCurrency, timeRange) = input
        val upcomingResult = upcomingAct(UpcomingAct.Input(baseCurrency = baseCurrency, range = timeRange))
        upcoming = DueSection(
            trns = upcomingResult.upcomingTrns.toImmutableList(),
            stats = upcomingResult.upcoming,
            expanded = upcoming.expanded
        )

        val overdueResult = overdueAct(OverdueAct.Input(baseCurrency = baseCurrency, toRange = timeRange.to))
        overdue = DueSection(
            trns = overdueResult.overdueTrns.toImmutableList(),
            stats = overdueResult.overdue,
            expanded = overdue.expanded
        )
    }

    private suspend fun loadCustomerJourney() {
        customerJourneyCards = ioThread {
            customerJourneyLogic.loadCards().toImmutableList()
        }
    }
// -----------------------------------------------------------------

    private fun setUpcomingExpanded(expanded: Boolean) {
        upcoming = upcoming.copy(expanded = expanded)
    }

    private fun setOverdueExpanded(expanded: Boolean) {
        overdue = overdue.copy(expanded = expanded)
    }

    private suspend fun onBalanceClick() {
        val hasTransactions = hasTrnsAct()
        if (hasTransactions) {
            // has transactions show him "Balance" screen
            nav.navigateTo(BalanceScreen)
        } else {
            // doesn't have transactions lead him to adjust balance
            mainScreenState.selectTab(MainTab.ACCOUNTS)
            nav.navigateTo(MainScreen)
        }
    }

    private suspend fun onHiddenBalanceClick() {
        hideBalance = false

        // Showing Balance fow 5s
        delay(5000)

        hideBalance = true
    }

    private suspend fun onHiddenIncomeClick() {
        hideIncome = false

        // Showing Balance fow 5s
        delay(5000)

        hideIncome = true
    }

    private fun switchTheme() {
        viewModelScope.launch {
            val theme = themeState.theme.next()
            appPreferences.setTheme(theme)
            themeState.switchTheme(theme)
            currentTheme = theme
        }
    }

    private fun setBuffer(newBuffer: Double) {
        viewModelScope.launch {
            appPreferences.setBufferAmount(newBuffer)
            buffer = buffer.copy(amount = newBuffer.toBigDecimal())
        }
    }

    private suspend fun payOrGetPlanned(transaction: Transaction) {
        dueTransactionsUseCase.pay(transaction)
        reload()
    }

    private suspend fun skipPlanned(transaction: Transaction) {
        skipAllPlanned(listOf(transaction))
    }

    private suspend fun skipAllPlanned(transactions: List<Transaction>) {
        dueTransactionsUseCase.skip(transactions)
        reload()
    }

    private suspend fun dismissCustomerJourneyCard(card: CustomerJourneyCardModel) {
        customerJourneyLogic.dismissCard(card)
        reload()
    }

    private suspend fun onSelectNextMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        val period = month?.let { periodState.selectMonth(it, 1L, year) }
        if (period != null) {
            setPeriod(period)
        }
    }

    private suspend fun onSelectPreviousMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        val period = month?.let { periodState.selectMonth(it, -1L, year) }
        if (period != null) {
            setPeriod(period)
        }
    }

    private suspend fun setPeriod(period: TimePeriod) {
        reload(period)
    }

    @JvmName("setExpandedMethod")
    private fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
    }
}
