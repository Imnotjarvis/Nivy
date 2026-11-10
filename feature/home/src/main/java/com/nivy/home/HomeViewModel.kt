package com.nivy.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.base.model.Theme
import com.nivy.base.model.next
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.model.Account
import com.nivy.data.model.Transaction
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.AppSettings
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.features.Features
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.domain.usecase.wallet.WalletIncomeExpenseUseCase
import com.nivy.home.customerjourney.CustomerJourneyCardModel
import com.nivy.home.customerjourney.CustomerJourneyCardsProvider
import com.nivy.home.usecase.Glance
import com.nivy.home.usecase.HistoryWithDateDividersUseCase
import com.nivy.home.usecase.HomeGlanceUseCase
import com.nivy.home.usecase.OverdueTransactionsUseCase
import com.nivy.home.usecase.UpcomingTransactionsUseCase
import com.nivy.navigation.BalanceScreen
import com.nivy.navigation.MainScreen
import com.nivy.navigation.Navigation
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.LoadTimer
import com.nivy.ui.R
import com.nivy.wallet.MainScreenState
import com.nivy.wallet.PeriodState
import com.nivy.wallet.ThemeState
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.data.DueSection
import com.nivy.wallet.data.model.MainTab
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.data.model.toUTCCloseTimeRange
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.domain.usecase.StartDayOfMonthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

// How long a tap shows a hidden balance or income.
private val RevealHiddenFor = 5.seconds

@Stable
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val periodState: PeriodState,
    private val themeState: ThemeState,
    private val mainScreenState: MainScreenState,
    private val nav: Navigation,
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val customerJourneyLogic: CustomerJourneyCardsProvider,
    private val historyWithDateDividersUseCase: HistoryWithDateDividersUseCase,
    private val walletIncomeExpenseUseCase: WalletIncomeExpenseUseCase,
    private val walletBalanceUseCase: WalletBalanceUseCase,
    private val appPreferences: AppPreferences,
    private val currencyRepository: CurrencyRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val upcomingTransactionsUseCase: UpcomingTransactionsUseCase,
    private val overdueTransactionsUseCase: OverdueTransactionsUseCase,
    private val homeGlanceUseCase: HomeGlanceUseCase,
    private val transactionDao: TransactionDao,
    private val startDayOfMonthUseCase: StartDayOfMonthUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val features: Features,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val userWrites: UserWrites,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<HomeState, HomeEvent>() {
    private var currentTheme by mutableStateOf(Theme.AUTO)
    private var period by mutableStateOf(periodState.selected)
    private var baseData by mutableStateOf(
        AppBaseData(
            baseCurrency = "",
            accounts = persistentListOf(),
            categories = persistentListOf()
        )
    )
    private var history by mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())

    // Once the month's transactions are listed for the first time: Home is fully drawn.
    private var historyLoaded by mutableStateOf(false)
    private var stats by mutableStateOf(IncomeExpensePair.zero())
    private var statsLoaded by mutableStateOf(false)
    private var balance by mutableStateOf(BigDecimal.ZERO)
    private var balanceLoaded by mutableStateOf(false)
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
    private var glance by mutableStateOf<Glance?>(null)
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
            theme = currentTheme,
            period = period,
            baseData = baseData,
            history = history,
            historyLoaded = historyLoaded,
            stats = stats,
            statsLoaded = statsLoaded,
            balance = balance,
            balanceLoaded = balanceLoaded,
            upcoming = upcoming,
            overdue = overdue,
            glance = glance,
            customerJourneyCards = customerJourneyCards,
            hideBalance = hideBalance,
            expanded = expanded,
            hideIncome = hideIncome,
            shouldShowAccountSpecificColorInTransactions = getShouldShowAccountSpecificColorInTransactions(),
            compactTransactions = features.compactTransactionsMode.asEnabledState(),
        )
    }

    @Composable
    fun getShouldShowAccountSpecificColorInTransactions(): Boolean {
        return features.showAccountColorsInTransactions.asEnabledState()
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
                HomeEvent.SwitchTheme -> switchTheme()
                is HomeEvent.DismissCustomerJourneyCard -> dismissCustomerJourneyCard(event.card)
                is HomeEvent.SetExpanded -> setExpanded(event.expanded)
            }
        }
    }

    private suspend fun start() {
        val startDay = startDayOfMonthUseCase.startDay()
        reload(periodState.initSelected(startDayOfMonth = startDay))
    }

    private var loadJob: Job? = null

    /**
     * Loads [timePeriod]. A newer load cancels one still running: choosing two months quickly, the
     * first one's transactions would otherwise come in after the second's and show under it.
     */
    private suspend fun reload(
        timePeriod: TimePeriod = periodState.selected
    ) {
        loadJob?.cancel()
        val load = viewModelScope.launch { load(timePeriod) }
        loadJob = load
        load.join()
    }

    private suspend fun load(timePeriod: TimePeriod) {
        val timer = LoadTimer("Home")
        val settings = timer.step("settings") {
            val stored = appPreferences.settings()
            AppSettings(
                theme = stored?.theme ?: themeState.theme,
                baseCurrency = currencyRepository.getBaseCurrency().code,
                bufferAmount = stored?.bufferAmount ?: 0.0,
                name = stored?.name.orEmpty(),
            )
        }
        val hideBalance = appPreferences.hideCurrentBalance()
        val hideIncome = appPreferences.hideIncome()

        currentTheme = settings.theme
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

        val withAccounts = timer.step("accounts") { loadAppBaseData(settings to timeRange) }
        val currencyAndRange = settings.baseCurrency to timeRange
        // First what shows above the transactions, so that nothing comes in over them and pushes
        // them down; then the transactions, and the header's glance, which moves nothing; last the
        // balance, which reads every transaction there is.
        timer.step("cards") { loadCustomerJourney() }
        timer.step("planned") { loadDueTrns(currencyAndRange) }
        timer.step("month totals") { loadMonthTotals(withAccounts) }
        timer.step("transactions") { loadTrnHistory(currencyAndRange) }
        timer.step("glance") { loadGlance(withAccounts) }
        timer.step("balance") { loadBalance(settings.baseCurrency) }
        timer.done()
        // Once all Home shows has loaded, which the download and the rates it rewrites would slow.
        viewModelScope.launch(dispatchers.io) {
            syncExchangeRatesUseCase.syncIfDue(currencyRepository.getBaseCurrency())
        }
    }

    private suspend fun loadAppBaseData(
        input: Pair<AppSettings, ClosedTimeRange>
    ): Triple<AppSettings, ClosedTimeRange, List<Account>> {
        val (settings, timeRange) = input
        val accounts = accountRepository.findAll()
        val categories = categoryRepository.findAll()

        baseData = AppBaseData(
            baseCurrency = settings.baseCurrency,
            categories = categories.toImmutableList(),
            accounts = accounts.toImmutableList()
        )

        return Triple(settings, timeRange, accounts)
    }

    private suspend fun loadMonthTotals(input: Triple<AppSettings, ClosedTimeRange, List<Account>>) {
        val (settings, timeRange, accounts) = input
        stats = walletIncomeExpenseUseCase.incomeExpense(
            baseCurrency = settings.baseCurrency,
            accounts = accounts,
            range = timeRange
        )
        statsLoaded = true
    }

    private suspend fun loadGlance(input: Triple<AppSettings, ClosedTimeRange, List<Account>>) {
        val (settings, _, accounts) = input
        glance = homeGlanceUseCase.glance(
            baseCurrency = settings.baseCurrency,
            accounts = accounts,
            categories = baseData.categories,
        )
    }

    private suspend fun loadBalance(baseCurrency: String) {
        balance = walletBalanceUseCase.balance(baseCurrency = baseCurrency)
        balanceLoaded = true
    }

    private suspend fun loadTrnHistory(input: Pair<String, ClosedTimeRange>) {
        val (baseCurrency, timeRange) = input

        history = historyWithDateDividersUseCase.history(
            range = timeRange,
            baseCurrency = baseCurrency
        )
        historyLoaded = true
    }

    private suspend fun loadDueTrns(input: Pair<String, ClosedTimeRange>) {
        val (baseCurrency, timeRange) = input
        val upcomingResult = upcomingTransactionsUseCase.upcoming(baseCurrency = baseCurrency, range = timeRange)
        upcoming = DueSection(
            trns = upcomingResult.upcomingTrns.toImmutableList(),
            stats = upcomingResult.upcoming,
            expanded = upcoming.expanded
        )

        val overdueResult = overdueTransactionsUseCase.overdue(baseCurrency = baseCurrency, toRange = timeRange.to)
        overdue = DueSection(
            trns = overdueResult.overdueTrns.toImmutableList(),
            stats = overdueResult.overdue,
            expanded = overdue.expanded
        )
    }

    private suspend fun loadCustomerJourney() {
        customerJourneyCards = withContext(dispatchers.io) {
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
        val hasTransactions = transactionDao.hasTransactions()
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

        delay(RevealHiddenFor)

        hideBalance = true
    }

    private suspend fun onHiddenIncomeClick() {
        hideIncome = false

        delay(RevealHiddenFor)

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

    private suspend fun payOrGetPlanned(transaction: Transaction) {
        if (userWrites.run(R.string.msg_change_not_saved) { dueTransactionsUseCase.pay(transaction) }) reload()
    }

    private suspend fun skipPlanned(transaction: Transaction) {
        skipAllPlanned(listOf(transaction))
    }

    private suspend fun skipAllPlanned(transactions: List<Transaction>) {
        if (userWrites.run(R.string.msg_change_not_saved) { dueTransactionsUseCase.skip(transactions) }) reload()
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
