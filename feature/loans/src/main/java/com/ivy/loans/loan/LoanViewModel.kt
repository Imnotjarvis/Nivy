package com.ivy.loans.loan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import com.ivy.base.model.processByType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Loan
import com.ivy.data.model.LoanId
import com.ivy.base.model.LoanType
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.LoanRepository
import com.ivy.domain.usecase.loan.LoanTransactionsUseCase
import com.ivy.wallet.domain.logic.AccountCreator
import com.ivy.wallet.utils.format
import com.ivy.wallet.utils.getDefaultFIATCurrency
import com.ivy.wallet.utils.ioThread
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.time.impl.DateTimePicker
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.loan.LoansAct
import com.ivy.wallet.domain.logic.model.CreateAccountData
import com.ivy.wallet.domain.logic.model.CreateLoanData
import com.ivy.wallet.ui.theme.modal.LoanModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

private const val Percent = 100

@Stable
@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val currencyRepository: CurrencyRepository,
    private val appPreferences: AppPreferences,
    private val accountCreator: AccountCreator,
    private val loanTransactionsUseCase: LoanTransactionsUseCase,
    private val loansAct: LoansAct,
    private val accountsAct: AccountsAct,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val dateTimePicker: DateTimePicker
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

    /** If true paid off loans will be visible */
    private var paidOffLoanVisibility by mutableStateOf(true)

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
            baseCurrency = baseCurrencyCode,
            accounts = accounts,
            selectedAccount = selectedAccount,
            loanModalData = loanModalData,
            reorderModalVisible = reorderModalVisible,
            totalOweAmount = getTotalOweAmount(totalOweAmount, defaultCurrencyCode),
            totalOwedAmount = getTotalOwedAmount(totalOwedAmount, defaultCurrencyCode),
            paidOffLoanVisibility = paidOffLoanVisibility,
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

            LoanScreenEvent.OnTogglePaidOffLoanVisibility -> {
                updatePaidOffLoanVisibility()
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
        viewModelScope.launch(Dispatchers.Default) {
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
                loansAct()
                    .map { loan ->
                        val (amountPaid, loanTotalAmount) = calculateAmountPaidAndTotalAmount(loan)
                        val percentPaid = if (loanTotalAmount != 0.0) {
                            amountPaid / loanTotalAmount
                        } else {
                            0.0
                        }
                        val currCode = findCurrencyCode(accounts, loan.account?.value)

                        when (loan.type) {
                            LoanType.BORROW -> totalOweAmount += (loanTotalAmount - amountPaid)
                            LoanType.LEND -> totalOwedAmount += (loanTotalAmount - amountPaid)
                        }

                        DisplayLoan(
                            loan = loan,
                            loanTotalAmount = loanTotalAmount,
                            amountPaid = amountPaid,
                            currencyCode = currCode,
                            formattedDisplayText = "${amountPaid.format(currCode)} $currCode / ${
                                loanTotalAmount.format(
                                    currCode
                                )
                            } $currCode (${
                                percentPaid.times(
                                    Percent
                                ).format(2)
                            }%)",
                            percentPaid = percentPaid
                        )
                    }.toImmutableList()
            }
            loadPendingLoans()
            loadCompletedLoans()
        }
    }

    private fun getTotalOwedAmount(totalOwedAmount: Double, currCode: String): String {
        return if (totalOwedAmount != 0.0) {
            "${totalOwedAmount.format(currCode)} $currCode"
        } else {
            ""
        }
    }

    private fun getTotalOweAmount(totalOweAmount: Double, currCode: String): String {
        return if (totalOweAmount != 0.0) {
            "${totalOweAmount.format(currCode)} $currCode"
        } else {
            ""
        }
    }

    private suspend fun initialiseAccounts() {
        val accountsList = accountsAct()
        accounts = accountsList
        selectedAccount = defaultAccountId(accountsList)
        selectedAccount?.let {
            baseCurrencyCode = it.asset.code
        }
    }

    private fun handleChangeDate() {
        dateTimePicker.pickDate(
            initialDate = loanModalData?.loan?.dateTime?.let {
                with(timeConverter) { it.toUTC() }
            } ?: timeProvider.utcNow()
        ) { localDate ->
            val localTime = loanModalData?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleChangeTime() {
        dateTimePicker.pickTime(
            initialTime = loanModalData?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()
        ) { localTime ->
            val localDate = loanModalData?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: timeProvider.localDateNow()

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun updateDateTime(newDateTime: LocalDateTime) {
        val newDateTimeUtc = with(timeConverter) { newDateTime.toUTC() }
        loanModalData?.let { currentData ->
            loanModalData = currentData.copy(
                loan = currentData.loan?.copy(
                    dateTime = newDateTime
                )
            )
            dateTime = newDateTimeUtc
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
            loanRepository.save(loan)
            loanTransactionsUseCase.syncLoanTransaction(loan, create = data.createLoanTransaction)
            start()
        }
    }

    private fun reorder(newOrder: List<DisplayLoan>) {
        viewModelScope.launch {
            loanRepository.saveMany(
                newOrder.mapIndexed { index, item -> item.loan.copy(orderNum = index.toDouble()) }
            )
            start()
        }
    }

    private fun loadCompletedLoans() {
        completedLoans = allLoans.filter { loan -> loan.percentPaid >= 1.0 }.toImmutableList()
    }

    private fun loadPendingLoans() {
        pendingLoans = allLoans.filter { loan -> loan.percentPaid < 1.0 }.toImmutableList()
    }

    private fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            accountCreator.createAccount(data) {
                accounts = accountsAct()
            }
        }
    }

    private suspend fun defaultAccountId(
        accounts: List<Account>,
    ): Account? {
        val lastSelectedId = appPreferences.lastSelectedAccountId()?.let {
            UUID.fromString(it)
        }

        lastSelectedId?.let { uuid ->
            return accounts.find { it.id.value == uuid }
        } ?: run {
            return if (accounts.isNotEmpty()) accounts[0] else null
        }
    }

    private fun findCurrencyCode(accounts: List<Account>, accountId: UUID?): String {
        return accountId?.let {
            accounts.find { account -> account.id.value == it }?.asset?.code
        } ?: defaultCurrencyCode
    }

    /**
     *  Calculates the total amount paid and the total loan amount including any changes made to the loan.
     *  @return A Pair containing the total amount paid and the total loan amount.
     */
    private suspend fun calculateAmountPaidAndTotalAmount(loan: Loan): Pair<Double, Double> {
        val loanRecords = loanRepository.findRecordsByLoanId(loan.id)
        val (amountPaid, loanTotalAmount) = loanRecords.fold(0.0 to loan.amount.value) { value, loanRecord ->
            val (currentAmountPaid, currentLoanTotalAmount) = value
            if (loanRecord.interest) return@fold value
            val convertedAmount = (loanRecord.convertedAmount ?: loanRecord.amount).value

            loanRecord.type.processByType(
                decreaseAction = { currentAmountPaid + convertedAmount to currentLoanTotalAmount },
                increaseAction = { currentAmountPaid to currentLoanTotalAmount + convertedAmount }
            )
        }
        return amountPaid to loanTotalAmount
    }

    private fun updatePaidOffLoanVisibility() {
        paidOffLoanVisibility = paidOffLoanVisibility.not()
    }
}
