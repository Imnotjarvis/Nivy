package com.nivy.loans.loandetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import com.nivy.base.Toaster
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.loan.LoanTransactionsUseCase
import com.nivy.domain.usecase.loan.NoLoanExchangeRate
import com.nivy.loans.LoanModalData
import com.nivy.loans.LoanProgress
import com.nivy.loans.loan.data.DisplayLoanRecord
import com.nivy.loans.loandetails.events.DeleteLoanModalEvent
import com.nivy.loans.loandetails.events.LoanDetailsScreenEvent
import com.nivy.loans.loandetails.events.LoanModalEvent
import com.nivy.loans.loandetails.events.LoanRecordModalEvent
import com.nivy.loans.progress
import com.nivy.navigation.LoanDetailsScreen
import com.nivy.navigation.Navigation
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.ui.time.impl.DateTimePicker
import com.nivy.wallet.domain.logic.AccountCreator
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateLoanRecordData
import com.nivy.wallet.domain.logic.model.EditLoanRecordData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val accountRepository: AccountRepository,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val dateTimePicker: DateTimePicker,
    private val dispatchers: DispatchersProvider,
    private val toaster: Toaster,
    private val userWrites: UserWrites,
) : ComposeViewModel<LoanDetailsScreenState, LoanDetailsScreenEvent>() {

    private val baseCurrency = mutableStateOf("")
    private val loan = mutableStateOf<Loan?>(null)
    private val displayLoanRecords =
        mutableStateOf<ImmutableList<DisplayLoanRecord>>(persistentListOf())
    private val progress = mutableStateOf(LoanProgress.None)
    private val accounts = mutableStateOf<ImmutableList<Account>>(persistentListOf())
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
            progress = progress.value,
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

            defaultCurrencyCode = withContext(dispatchers.io) {
                currencyRepository.getBaseCurrency().code
            }.also {
                baseCurrency.value = it
            }

            accounts.value = accountRepository.findAll().toImmutableList()

            loan.value = loanRepository.findById(LoanId(loanId))

            loan.value?.let { loan ->
                selectedLoanAccount.value = accounts.value.find {
                    loan.account == it.id
                }

                selectedLoanAccount.value?.let { acc ->
                    baseCurrency.value = acc.asset.code
                }
            }

            withContext(dispatchers.default) {
                displayLoanRecords.value =
                    loanRepository.findRecordsByLoanId(LoanId(loanId)).map {
                        val trans = withContext(dispatchers.io) {
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

            progress.value = withContext(dispatchers.default) {
                loan.value?.progress(displayLoanRecords.value.map { it.loanRecord }) ?: LoanProgress.None
            }

            createLoanTransaction.value =
                transactionRepository.findLoanTransaction(loanId = loanId) != null
        }
    }

    fun editLoan(loan: Loan, createLoanTransaction: Boolean = false) {
        viewModelScope.launch {
            val oldAccount = this@LoanDetailsViewModel.loan.value?.account
            // Before the loan: when a record can't be converted, the loan stays as it was.
            val recalculated = try {
                userWrites.run(R.string.msg_change_not_saved) {
                    loanTransactionsUseCase.recalculateRecords(loan, oldAccount = oldAccount)
                }
            } catch (ignored: NoLoanExchangeRate) {
                toaster.show(R.string.msg_no_loan_exchange_rate)
                false
            }
            val saved = recalculated && userWrites.run(R.string.msg_change_not_saved) {
                loanRepository.save(loan)
                loanTransactionsUseCase.syncLoanTransaction(loan, create = createLoanTransaction)
                // A changed type turns the records' income into expenses and the other way round.
                loanTransactionsUseCase.syncRecordTransactions(loan)
            }
            if (saved) load(loanId = loan.id.value)
            // Shown while an account change recalculates the records; nothing else hides it.
            waitModalVisible.value = false
        }
    }

    private fun deleteLoan() {
        val loan = loan.value ?: return
        viewModelScope.launch {
            val deleted = userWrites.run(R.string.msg_change_not_saved) {
                loanTransactionsUseCase.deleteLoanTransactions(loan.id)
                loanRepository.deleteById(loan.id)
            }
            if (deleted) nav.back()
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
            val withConvertedAmount = try {
                record.copy(
                    convertedAmount = loanTransactionsUseCase.convertedAmount(
                        loanAccount = loan.account,
                        record = record,
                    )
                )
            } catch (ignored: NoLoanExchangeRate) {
                toaster.show(R.string.msg_no_loan_exchange_rate)
                return@launch
            }
            val saved = userWrites.run(R.string.msg_change_not_saved) {
                loanRepository.saveRecord(withConvertedAmount)
                loanTransactionsUseCase.syncLoanRecordTransaction(
                    loan = loan,
                    record = withConvertedAmount,
                    create = data.createLoanRecordTransaction,
                )
            }
            if (saved) load(loanId = loan.id.value)
        }
    }

    private fun editLoanRecord(editLoanRecordData: EditLoanRecordData) {
        val loan = loan.value ?: return
        viewModelScope.launch {
            val record = try {
                editLoanRecordData.newLoanRecord.copy(
                    convertedAmount = loanTransactionsUseCase.convertedAmount(
                        loanAccount = loan.account,
                        record = editLoanRecordData.newLoanRecord,
                        oldRecord = editLoanRecordData.originalLoanRecord,
                        recalculate = editLoanRecordData.reCalculateLoanAmount,
                    )
                )
            } catch (ignored: NoLoanExchangeRate) {
                toaster.show(R.string.msg_no_loan_exchange_rate)
                return@launch
            }
            val saved = userWrites.run(R.string.msg_change_not_saved) {
                loanRepository.saveRecord(record)
                loanTransactionsUseCase.syncLoanRecordTransaction(
                    loan = loan,
                    record = record,
                    create = editLoanRecordData.createLoanRecordTransaction,
                )
            }
            if (saved) load(loanId = loan.id.value)
        }
    }

    private fun deleteLoanRecord(loanRecord: LoanRecord) {
        val loanId = loan.value?.id ?: return
        viewModelScope.launch {
            val deleted = userWrites.run(R.string.msg_change_not_saved) {
                loanRepository.deleteRecordById(loanRecord.id)
                loanTransactionsUseCase.deleteLoanRecordTransaction(loanRecord.id)
            }
            if (deleted) load(loanId = loanId.value)
        }
    }

    private fun handleChangeDate() {
        dateTimePicker.pickDate(
            initialDate = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalDateTime().toUTC() }
            } ?: dateTime.value
        ) { localDate ->

            val localTime = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.value.toLocalTime() }

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleChangeTime() {
        dateTimePicker.pickTime(
            initialTime = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.value.toLocalTime() }
        ) { localTime ->
            val localDate = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: with(timeConverter) { dateTime.value.toLocalDate() }

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
            } ?: dateTime.value
        ) { localDate ->

            val localTime = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.value.toLocalTime() }

            updateLoanDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleLoanChangeTime() {
        dateTimePicker.pickTime(
            initialTime = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.value.toLocalTime() }
        ) { localTime ->
            val localDate = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: with(timeConverter) { dateTime.value.toLocalDate() }

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
                accounts.value = accountRepository.findAll().toImmutableList()
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
