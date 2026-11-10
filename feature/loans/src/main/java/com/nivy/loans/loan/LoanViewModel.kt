package com.nivy.loans.loan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import com.nivy.base.model.LoanType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.pickable
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.loan.LoanTransactionsUseCase
import com.nivy.loans.LoanModalData
import com.nivy.loans.loan.data.DisplayLoan
import com.nivy.loans.progress
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.ui.time.impl.DateTimePicker
import com.nivy.wallet.domain.logic.AccountCreator
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateLoanData
import com.nivy.wallet.domain.pure.account.startingAccount
import com.nivy.wallet.utils.amountAndCode
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.formatDecimals
import com.nivy.wallet.utils.getDefaultFIATCurrency
import com.nivy.wallet.utils.ioThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

private const val Percent = 100

@Stable
@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val currencyRepository: CurrencyRepository,
    private val appPreferences: AppPreferences,
    private val accountCreator: AccountCreator,
    private val loanTransactionsUseCase: LoanTransactionsUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val accountRepository: AccountRepository,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val dateTimePicker: DateTimePicker,
    private val dispatchers: DispatchersProvider,
    private val userWrites: UserWrites,
) : ComposeViewModel<LoanScreenState, LoanScreenEvent>() {

    private var baseCurrencyCode by mutableStateOf(getDefaultFIATCurrency().currencyCode)
    private var completedLoans by mutableStateOf<ImmutableList<DisplayLoan>>(persistentListOf())
    private var pendingLoans by mutableStateOf<ImmutableList<DisplayLoan>>(persistentListOf())
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var selectedAccount by mutableStateOf<Account?>(null)
    private var loanModalData by mutableStateOf<LoanModalData?>(null)
    private var reorderModalVisible by mutableStateOf(false)
    private var dateTime by mutableStateOf<Instant>(timeProvider.utcNow())
    private var selectedTab by mutableStateOf(LoanTab.PENDING)

    /** Contains all loans including both paidOff and pending*/
    private var allLoans: ImmutableList<DisplayLoan> = persistentListOf()
    private var defaultCurrencyCode = ""
    private var totalOweAmount = 0.0
    private var totalOwedAmount = 0.0

    @Composable
    override fun uiState(): LoanScreenState {
        LaunchedEffect(Unit) {
            start()
        }

        return LoanScreenState(
            accounts = accounts,
            loanModalData = loanModalData,
            reorderModalVisible = reorderModalVisible,
            totalOweAmount = totalText(totalOweAmount, defaultCurrencyCode),
            totalOwedAmount = totalText(totalOwedAmount, defaultCurrencyCode),
            dateTime = dateTime,
            selectedTab = selectedTab,
            completedLoans = completedLoans,
            pendingLoans = pendingLoans
        )
    }

    fun setTab(tab: LoanTab) {
        selectedTab = tab
    }

    override fun onEvent(event: LoanScreenEvent) {
        when (event) {
            is LoanScreenEvent.OnLoanCreate -> {
                createLoan(event.createLoanData)
            }

            is LoanScreenEvent.OnAddLoan -> {
                loanModalData = LoanModalData(
                    loan = null,
                    baseCurrency = baseCurrencyCode,
                    selectedAccount = selectedAccount
                )
            }

            is LoanScreenEvent.OnLoanModalDismiss -> {
                loanModalData = null
                dateTime = timeProvider.utcNow()
            }

            is LoanScreenEvent.OnReOrderModalShow -> {
                reorderModalVisible = event.show
            }

            is LoanScreenEvent.OnReordered -> {
                reorder(event.reorderedList)
            }

            is LoanScreenEvent.OnCreateAccount -> {
                createAccount(event.accountData)
            }

            is LoanScreenEvent.OnChangeDate -> {
                handleChangeDate()
            }

            is LoanScreenEvent.OnChangeTime -> {
                handleChangeTime()
            }

            is LoanScreenEvent.OnTabChanged -> {
                setTab(event.tab)
            }
        }
    }

    private fun start() {
        viewModelScope.launch(dispatchers.default) {
            dateTime = timeProvider.utcNow()

            defaultCurrencyCode = ioThread {
                currencyRepository.getBaseCurrency().code
            }.also {
                baseCurrencyCode = it
            }

            initialiseAccounts()

            totalOweAmount = 0.0
            totalOwedAmount = 0.0

            allLoans = ioThread {
                // Every account, archived ones too: the picker's list leaves those out.
                val allAccounts = accountRepository.findAll()
                loanRepository.findAll()
                    .map { loan ->
                        val progress = loan.progress(loanRepository.findRecordsByLoanId(loan.id))
                        val currCode = findCurrencyCode(allAccounts, loan.account?.value)

                        // The totals are shown in the base currency; each loan is in its account's.
                        val remaining = inBaseCurrency(progress.left, currCode)
                        when (loan.type) {
                            LoanType.BORROW -> totalOweAmount += remaining
                            LoanType.LEND -> totalOwedAmount += remaining
                        }

                        DisplayLoan(
                            loan = loan,
                            loanTotalAmount = progress.total,
                            amountPaid = progress.paid,
                            currencyCode = currCode,
                            formattedDisplayText = "${progress.paid.format(currCode)} $currCode / " +
                                "${progress.total.format(currCode)} $currCode " +
                                "(${progress.percentPaid.times(Percent).formatDecimals(2)}%)",
                            percentPaid = progress.percentPaid,
                            paidOff = progress.paidOff,
                        )
                    }.toImmutableList()
            }
            loadPendingLoans()
            loadCompletedLoans()
        }
    }

    // A total for the header, or nothing when it's zero.
    private fun totalText(total: Double, currCode: String): String =
        if (total != 0.0) amountAndCode(total, currCode) else ""

    private suspend fun initialiseAccounts() {
        val accountsList = accountRepository.findAll().pickable().toImmutableList()
        accounts = accountsList
        selectedAccount = defaultAccountId(accountsList)
        selectedAccount?.let {
            baseCurrencyCode = it.asset.code
        }
    }

    // This screen's dialog only adds loans: the date and time it changes are the new loan's.
    private fun handleChangeDate() {
        dateTimePicker.pickDate(initialDate = dateTime) { localDate ->
            updateDateTime(localDate.atTime(with(timeConverter) { dateTime.toLocalTime() }))
        }
    }

    private fun handleChangeTime() {
        dateTimePicker.pickTime(initialTime = with(timeConverter) { dateTime.toLocalTime() }) { localTime ->
            updateDateTime(with(timeConverter) { dateTime.toLocalDate() }.atTime(localTime))
        }
    }

    private fun updateDateTime(newDateTime: LocalDateTime) {
        if (loanModalData != null) {
            dateTime = with(timeConverter) { newDateTime.toUTC() }
        }
    }

    private fun createLoan(data: CreateLoanData) {
        viewModelScope.launch {
            val loan = either {
                Loan(
                    id = LoanId(UUID.randomUUID()),
                    name = NotBlankTrimmedString.from(data.name).bind(),
                    amount = PositiveDouble.from(data.amount).bind(),
                    type = data.type,
                    color = ColorInt(data.color.toArgb()),
                    icon = data.icon?.let(IconAsset::from)?.getOrNull(),
                    account = data.account?.id,
                    note = data.note?.let(NotBlankTrimmedString::from)?.getOrNull(),
                    dateTime = data.dateTime,
                    orderNum = loanRepository.findMaxOrderNum() + 1,
                )
            }.getOrNull() ?: return@launch
            val saved = userWrites.run(R.string.msg_change_not_saved) {
                // A new loan has no records to convert.
                loanTransactionsUseCase.saveLoan(
                    loan,
                    oldAccount = loan.account,
                    createTransaction = data.createLoanTransaction,
                )
            }
            if (saved) start()
        }
    }

    private fun reorder(newOrder: List<DisplayLoan>) {
        viewModelScope.launch {
            val saved = userWrites.run(R.string.msg_change_not_saved) {
                loanRepository.saveMany(
                    newOrder.mapIndexed { index, item -> item.loan.copy(orderNum = index.toDouble()) }
                )
            }
            if (saved) start()
        }
    }

    private fun loadCompletedLoans() {
        completedLoans = allLoans.filter { loan -> loan.paidOff }.toImmutableList()
    }

    private fun loadPendingLoans() {
        pendingLoans = allLoans.filterNot { loan -> loan.paidOff }.toImmutableList()
    }

    private fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            accountCreator.createAccount(data) {
                accounts = accountRepository.findAll().pickable().toImmutableList()
            }
        }
    }

    private suspend fun defaultAccountId(
        accounts: List<Account>,
    ): Account? = accounts.startingAccount(appPreferences.lastSelectedAccountId()?.let(UUID::fromString))

    /**
     * The amount in the base currency, or 0 when there's no rate for it, as budgets count it. An
     * amount below zero, a loan paid back beyond its total, stays below zero.
     */
    private suspend fun inBaseCurrency(amount: Double, currencyCode: String): Double {
        val size = PositiveDouble.from(abs(amount)).getOrNull() ?: return 0.0
        val converted = exchangeUseCase.convert(
            PositiveValue(amount = size, asset = AssetCode.unsafe(currencyCode)),
            to = AssetCode.unsafe(defaultCurrencyCode),
        ).getOrNull()?.amount?.value ?: return 0.0
        return if (amount < 0) -converted else converted
    }

    private fun findCurrencyCode(accounts: List<Account>, accountId: UUID?): String {
        return accountId?.let {
            accounts.find { account -> account.id.value == it }?.asset?.code
        } ?: defaultCurrencyCode
    }
}
