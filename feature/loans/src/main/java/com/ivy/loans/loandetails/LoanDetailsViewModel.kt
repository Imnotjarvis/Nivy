package com.ivy.loans.loandetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import com.ivy.base.model.LoanRecordType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Loan
import com.ivy.data.model.LoanId
import com.ivy.data.model.LoanRecord
import com.ivy.data.model.LoanRecordId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.LoanRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.loan.LoanTransactionsUseCase
import com.ivy.wallet.domain.logic.AccountCreator
import com.ivy.wallet.utils.computationThread
import com.ivy.wallet.utils.ioThread
import com.ivy.loans.loan.data.DisplayLoanRecord
import com.ivy.loans.loandetails.events.DeleteLoanModalEvent
import com.ivy.loans.loandetails.events.LoanDetailsScreenEvent
import com.ivy.loans.loandetails.events.LoanModalEvent
import com.ivy.loans.loandetails.events.LoanRecordModalEvent
import com.ivy.navigation.LoanDetailsScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.time.impl.DateTimePicker
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.loan.LoanByIdAct
import com.ivy.wallet.domain.logic.model.CreateAccountData
import com.ivy.wallet.domain.logic.model.CreateLoanRecordData
import com.ivy.wallet.domain.logic.model.EditLoanRecordData
import com.ivy.wallet.ui.theme.modal.LoanModalData
import com.ivy.wallet.ui.theme.modal.LoanRecordModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Stable
@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionRepository: TransactionRepository,
    private val accountCreator: AccountCreator,
    private val loanTransactionsUseCase: LoanTransactionsUseCase,
    private val nav: Navigation,
    private val accountsAct: AccountsAct,
    private val loanByIdAct: LoanByIdAct,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val dateTimePicker: DateTimePicker,
) : ComposeViewModel<LoanDetailsScreenState, LoanDetailsScreenEvent>() {

    private val baseCurrency = mutableStateOf("")
    private val loan = mutableStateOf<Loan?>(null)
    private val displayLoanRecords =
        mutableStateOf<ImmutableList<DisplayLoanRecord>>(persistentListOf())
    private val loanTotalAmount = mutableDoubleStateOf(0.0)
    private val amountPaid = mutableDoubleStateOf(0.0)
    private val accounts = mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private val loanInterestAmountPaid = mutableDoubleStateOf(0.0)
    private val selectedLoanAccount = mutableStateOf<Account?>(null)
    private val createLoanTransaction = mutableStateOf(false)
    private var defaultCurrencyCode = ""
    private val loanModalData = mutableStateOf<LoanModalData?>(null)
    private val loanRecordModalData = mutableStateOf<LoanRecordModalData?>(null)
    private val waitModalVisible = mutableStateOf(false)
    private val isDeleteModalVisible = mutableStateOf(false)
    private var dateTime = mutableStateOf<Instant>(timeProvider.utcNow())
    lateinit var screen: LoanDetailsScreen

    @Composable
    override fun uiState(): LoanDetailsScreenState {
        LaunchedEffect(Unit) {
            start()
        }

        return LoanDetailsScreenState(
            baseCurrency = baseCurrency.value,
            loan = loan.value,
            displayLoanRecords = displayLoanRecords.value,
            loanTotalAmount = loanTotalAmount.doubleValue,
            amountPaid = amountPaid.doubleValue,
            loanAmountPaid = loanInterestAmountPaid.doubleValue,
            accounts = accounts.value,
            selectedLoanAccount = selectedLoanAccount.value,
            createLoanTransaction = createLoanTransaction.value,
            loanModalData = loanModalData.value,
            loanRecordModalData = loanRecordModalData.value,
            waitModalVisible = waitModalVisible.value,
            isDeleteModalVisible = isDeleteModalVisible.value,
            dateTime = dateTime.value
        )
    }

    override fun onEvent(event: LoanDetailsScreenEvent) {
        when (event) {
            is LoanRecordModalEvent -> handleLoanRecordModalEvents(event)
            is LoanModalEvent -> handleLoanModalEvents(event)
            is DeleteLoanModalEvent -> handleDeleteLoanModalEvents(event)
            is LoanDetailsScreenEvent -> handleLoanDetailsScreenEvents(event)
        }
    }

    private fun handleLoanRecordModalEvents(event: LoanDetailsScreenEvent) {
        when (event) {
            is LoanRecordModalEvent.OnClickLoanRecord -> {
                loanRecordModalData.value = LoanRecordModalData(
                    loanRecord = event.displayLoanRecord.loanRecord,
                    baseCurrency = event.displayLoanRecord.loanRecordCurrencyCode,
                    selectedAccount = event.displayLoanRecord.account,
                    createLoanRecordTransaction = event.displayLoanRecord.loanRecordTransaction,
                    isLoanInterest = event.displayLoanRecord.loanRecord.interest,
                    loanAccountCurrencyCode = event.displayLoanRecord.loanCurrencyCode
                )
            }

            is LoanRecordModalEvent.OnCreateLoanRecord -> {
                createLoanRecord(event.loanRecordData)
            }

            is LoanRecordModalEvent.OnDeleteLoanRecord -> {
                deleteLoanRecord(event.loanRecord)
            }

            LoanRecordModalEvent.OnDismissLoanRecord -> {
                loanRecordModalData.value = null
                dateTime.value = timeProvider.utcNow()
            }

            is LoanRecordModalEvent.OnEditLoanRecord -> {
                editLoanRecord(event.loanRecordData)
            }

            is LoanRecordModalEvent.OnChangeDate -> {
                handleChangeDate()
            }
            is LoanRecordModalEvent.OnChangeTime -> {
                handleChangeTime()
            }
            else -> {}
        }
    }

    private fun handleLoanModalEvents(event: LoanDetailsScreenEvent) {
        when (event) {
            LoanModalEvent.OnDismissLoanModal -> {
                loanModalData.value = null
                dateTime.value = timeProvider.utcNow()
            }

            is LoanModalEvent.OnEditLoanModal -> {
                editLoan(event.loan, event.createLoanTransaction)
            }

            LoanModalEvent.PerformCalculation -> {
                waitModalVisible.value = true
            }

            LoanModalEvent.OnChangeDate -> {
                handleLoanChangeDate()
            }

            LoanModalEvent.OnChangeTime -> {
                handleLoanChangeTime()
            }

            else -> {}
        }
    }

    private fun handleDeleteLoanModalEvents(event: LoanDetailsScreenEvent) {
        when (event) {
            DeleteLoanModalEvent.OnDeleteLoan -> {
                deleteLoan()
                isDeleteModalVisible.value = false
            }

            is DeleteLoanModalEvent.OnDismissDeleteLoan -> {
                isDeleteModalVisible.value = event.isDeleteModalVisible
            }

            else -> {}
        }
    }

    private fun handleLoanDetailsScreenEvents(event: LoanDetailsScreenEvent) {
        when (event) {
            LoanDetailsScreenEvent.OnAmountClick -> {
                loanModalData.value = LoanModalData(
                    loan = loan.value,
                    baseCurrency = baseCurrency.value,
                    autoFocusKeyboard = false,
                    autoOpenAmountModal = true,
                    selectedAccount = selectedLoanAccount.value,
                    createLoanTransaction = createLoanTransaction.value
                )
            }

            LoanDetailsScreenEvent.OnEditLoanClick -> {
                loanModalData.value = LoanModalData(
                    loan = loan.value,
                    baseCurrency = baseCurrency.value,
                    autoFocusKeyboard = false,
                    selectedAccount = selectedLoanAccount.value,
                    createLoanTransaction = createLoanTransaction.value
                )
            }

            LoanDetailsScreenEvent.OnAddRecord -> {
                loanRecordModalData.value = LoanRecordModalData(
                    loanRecord = null,
                    baseCurrency = baseCurrency.value,
                    selectedAccount = selectedLoanAccount.value
                )
            }

            is LoanDetailsScreenEvent.OnCreateAccount -> {
                createAccount(event.data)
            }

            else -> {}
        }
    }

    private fun start() {
        load(loanId = screen.loanId)
    }

    private fun load(loanId: UUID) {
        viewModelScope.launch {
            dateTime.value = timeProvider.utcNow()

            defaultCurrencyCode = ioThread {
                currencyRepository.getBaseCurrency().code
            }.also {
                baseCurrency.value = it
            }

            accounts.value = accountsAct()

            loan.value = loanByIdAct(loanId)

            loan.value?.let { loan ->
                selectedLoanAccount.value = accounts.value.find {
                    loan.account == it.id
                }

                selectedLoanAccount.value?.let { acc ->
                    baseCurrency.value = acc.asset.code
                }
            }

            computationThread {
                displayLoanRecords.value =
                    loanRepository.findRecordsByLoanId(LoanId(loanId)).map {
                        val trans = ioThread {
                            transactionRepository.findLoanRecordTransaction(
                                it.id.value
                            )
                        }

                        val account = findAccount(
                            accounts = accounts.value,
                            accountId = it.account?.value,
                        )

                        DisplayLoanRecord(
                            it,
                            account = account,
                            loanRecordTransaction = trans != null,
                            loanRecordCurrencyCode = account?.asset?.code ?: defaultCurrencyCode,
                            loanCurrencyCode = selectedLoanAccount.value?.asset?.code
                                ?: defaultCurrencyCode
                        )
                    }.toImmutableList()
            }

            computationThread {
                // Using a local variable to calculate the amount and then reassigning to
                // the State variable to reduce the amount of compose re-draws
                var amtPaid = 0.0
                var loanInterestAmtPaid = 0.0
                displayLoanRecords.value.forEach {
                    // We do not want to calculate records that increase loan.
                    if (it.loanRecord.type == LoanRecordType.INCREASE) {
                        return@forEach
                    }
                    val convertedAmount = (it.loanRecord.convertedAmount ?: it.loanRecord.amount).value
                    if (!it.loanRecord.interest) {
                        amtPaid += convertedAmount
                    } else {
                        loanInterestAmtPaid += convertedAmount
                    }
                }

                amountPaid.doubleValue = amtPaid
                loanInterestAmountPaid.doubleValue = loanInterestAmtPaid
            }

            computationThread {
                // Calculate total amount of loan borrowed or lent.
                // That is initial amount + each record that increased the loan.
                val totalAmount =
                    displayLoanRecords.value.fold(loan.value?.amount?.value ?: 0.0) { value, record ->
                        if (record.loanRecord.type == LoanRecordType.INCREASE) {
                            val convertedAmount =
                                (record.loanRecord.convertedAmount ?: record.loanRecord.amount).value
                            value + convertedAmount
                        } else {
                            value
                        }
                    }
                loanTotalAmount.doubleValue = totalAmount
            }

            createLoanTransaction.value =
                transactionRepository.findLoanTransaction(loanId = loanId) != null
        }
    }

    fun editLoan(loan: Loan, createLoanTransaction: Boolean = false) {
        viewModelScope.launch {
            val oldAccount = this@LoanDetailsViewModel.loan.value?.account
            loanRepository.save(loan)
            loanTransactionsUseCase.recalculateRecords(loan, oldAccount = oldAccount)
            loanTransactionsUseCase.syncLoanTransaction(loan, create = createLoanTransaction)
            load(loanId = loan.id.value)
        }
    }

    private fun deleteLoan() {
        val loan = loan.value ?: return
        viewModelScope.launch {
            loanTransactionsUseCase.deleteLoanTransactions(loan.id)
            loanRepository.deleteById(loan.id)
            // close screen
            nav.back()
        }
    }

    private fun createLoanRecord(data: CreateLoanRecordData) {
        val loan = loan.value ?: return
        viewModelScope.launch {
            val record = either {
                LoanRecord(
                    id = LoanRecordId(UUID.randomUUID()),
                    loan = loan.id,
                    amount = PositiveDouble.from(data.amount).bind(),
                    note = data.note?.let(NotBlankTrimmedString::from)?.getOrNull(),
                    dateTime = data.dateTime,
                    interest = data.interest,
                    account = data.account?.id,
                    convertedAmount = null,
                    type = data.loanRecordType,
                )
            }.getOrNull() ?: return@launch
            val withConvertedAmount = record.copy(
                convertedAmount = loanTransactionsUseCase.convertedAmount(
                    loanAccount = loan.account,
                    record = record,
                )
            )
            loanRepository.saveRecord(withConvertedAmount)
            loanTransactionsUseCase.syncLoanRecordTransaction(
                loan = loan,
                record = withConvertedAmount,
                create = data.createLoanRecordTransaction,
            )
            load(loanId = loan.id.value)
        }
    }

    private fun editLoanRecord(editLoanRecordData: EditLoanRecordData) {
        val loan = loan.value ?: return
        viewModelScope.launch {
            val record = editLoanRecordData.newLoanRecord.copy(
                convertedAmount = loanTransactionsUseCase.convertedAmount(
                    loanAccount = loan.account,
                    record = editLoanRecordData.newLoanRecord,
                    oldRecord = editLoanRecordData.originalLoanRecord,
                    recalculate = editLoanRecordData.reCalculateLoanAmount,
                )
            )
            loanRepository.saveRecord(record)
            loanTransactionsUseCase.syncLoanRecordTransaction(
                loan = loan,
                record = record,
                create = editLoanRecordData.createLoanRecordTransaction,
            )
            load(loanId = loan.id.value)
        }
    }

    private fun deleteLoanRecord(loanRecord: LoanRecord) {
        val loanId = loan.value?.id ?: return
        viewModelScope.launch {
            loanRepository.deleteRecordById(loanRecord.id)
            loanTransactionsUseCase.deleteLoanRecordTransaction(loanRecord.id)
            load(loanId = loanId.value)
        }
    }

    private fun handleChangeDate() {
        dateTimePicker.pickDate(
            initialDate = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalDateTime().toUTC() }
            } ?: timeProvider.utcNow()
        ) { localDate ->

            val localTime = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleChangeTime() {
        dateTimePicker.pickTime(
            initialTime = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()
        ) { localTime ->
            val localDate = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: timeProvider.localDateNow()

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun updateDateTime(newDateTime: LocalDateTime) {
        val newDateTimeUtc = with(timeConverter) { newDateTime.toUTC() }
        loanRecordModalData.value?.let { currentData ->
            loanRecordModalData.value = currentData.copy(
                loanRecord = currentData.loanRecord?.copy(
                    dateTime = newDateTimeUtc
                )
            )
            dateTime.value = newDateTimeUtc
        }
    }

    private fun handleLoanChangeDate() {
        dateTimePicker.pickDate(
            initialDate = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toUTC() }
            } ?: timeProvider.utcNow()
        ) { localDate ->

            val localTime = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()

            updateLoanDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleLoanChangeTime() {
        dateTimePicker.pickTime(
            initialTime = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()
        ) { localTime ->
            val localDate = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: timeProvider.localDateNow()

            updateLoanDateTime(localDate.atTime(localTime))
        }
    }

    private fun updateLoanDateTime(newDateTime: LocalDateTime) {
        val newDateTimeUtc = with(timeConverter) { newDateTime.toUTC() }
        loanModalData.value?.let { currentData ->
            loanModalData.value = currentData.copy(
                loan = currentData.loan?.copy(
                    dateTime = newDateTime
                )
            )
            dateTime.value = newDateTimeUtc
        }
    }

    private fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            accountCreator.createAccount(data) {
                accounts.value = accountsAct()
            }
        }
    }

    private fun findAccount(
        accounts: List<Account>,
        accountId: UUID?,
    ): Account? {
        return accountId?.let { uuid ->
            accounts.find { acc ->
                acc.id.value == uuid
            }
        }
    }
}
