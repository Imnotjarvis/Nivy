package com.ivy.transaction

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.Toaster
import com.ivy.base.model.AccountType
import com.ivy.base.model.TransactionType
import com.ivy.base.refreshWidget
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CreditCard
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.pickable
import com.ivy.data.model.primitive.AssociationId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TagMapper
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.ivy.domain.usecase.creditcard.chargeDateTime
import com.ivy.domain.usecase.creditcard.minChargeShift
import com.ivy.domain.features.Features
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.domain.usecase.loan.LoanTransactionsUseCase
import com.ivy.domain.usecase.planned.DueTransactionsUseCase
import com.ivy.wallet.data.EditTransactionDisplayLoan
import com.ivy.wallet.domain.logic.AccountCreator
import com.ivy.wallet.utils.computationThread
import com.ivy.wallet.utils.ioThread
import com.ivy.wallet.utils.toLowerCaseLocal
import com.ivy.wallet.utils.uiThread
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.R
import com.ivy.ui.time.impl.DateTimePicker
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.data.CustomExchangeRateState
import com.ivy.wallet.domain.logic.CategoryCreator
import com.ivy.wallet.domain.logic.SmartTitleSuggestionsLogic
import com.ivy.wallet.domain.logic.model.CreateAccountData
import com.ivy.wallet.domain.logic.model.CreateCategoryData
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Suppress("LargeClass")
@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val toaster: Toaster,
    private val loanDao: LoanDao,
    private val currencyRepository: CurrencyRepository,
    private val nav: Navigation,
    private val appPreferences: AppPreferences,
    private val exchangeUseCase: ExchangeUseCase,
    private val categoryCreator: CategoryCreator,
    private val accountCreator: AccountCreator,
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val smartTitleSuggestionsLogic: SmartTitleSuggestionsLogic,
    private val loanTransactionsUseCase: LoanTransactionsUseCase,
    private val accountsAct: AccountsAct,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepo: TransactionRepository,
    private val transactionMapper: TransactionMapper,
    private val tagRepository: TagRepository,
    private val tagMapper: TagMapper,
    private val features: Features,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val dateTimePicker: DateTimePicker,
    private val creditCardChargeUseCase: CreditCardChargeUseCase,
    private val creditCardRepository: CreditCardRepository,
) : ComposeViewModel<EditTransactionViewState, EditTransactionViewEvent>() {

    private var transactionType by mutableStateOf(TransactionType.EXPENSE)
    private var initialTitle by mutableStateOf<String?>(null)
    private var titleSuggestions by mutableStateOf(persistentSetOf<String>())
    private var currency by mutableStateOf("")
    private var description by mutableStateOf<String?>(null)
    private var dateTime by mutableStateOf<Instant?>(null)
    private var dueDate by mutableStateOf<Instant?>(null)
    private var chargeShift by mutableIntStateOf(0)
    private var allCreditCards by mutableStateOf<List<CreditCard>>(emptyList())
    private var creditCard by mutableStateOf<CreditCard?>(null)
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var categories by mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private var tags by mutableStateOf<ImmutableList<Tag>>(persistentListOf())
    private var transactionAssociatedTags by mutableStateOf<ImmutableList<TagId>>(persistentListOf())
    private var account by mutableStateOf<Account?>(null)
    private var toAccount by mutableStateOf<Account?>(null)
    private var category by mutableStateOf<Category?>(null)
    private var amount by mutableDoubleStateOf(0.0)
    private var hasChanges by mutableStateOf(false)
    private var displayLoanHelper by mutableStateOf(EditTransactionDisplayLoan())

    private var paidHistory: Instant? = null

    // This is used to when the transaction is associated with a loan/loan record,
    // used to indicate the background updating of loan/loanRecord data
    private var backgroundProcessingStarted by mutableStateOf(false)

    private var customExchangeRateState by mutableStateOf(CustomExchangeRateState())

    private var loadedTransaction: TransactionDraft? = null
    private var editMode = false

    private var title: String? = null
    private lateinit var baseUserCurrency: String
    private var tagSearchJob: Job? = null
    private val tagSearchDebounceTimeInMills: Long = 500

    fun start(screen: EditTransactionScreen) {
        viewModelScope.launch {
            editMode = screen.initialTransactionId != null

            baseUserCurrency = baseCurrency()

            val tagList = async { getAllTags() }

            val getAccounts = accountsAct()
            if (getAccounts.isEmpty()) {
                closeScreen()
                return@launch
            }

            categories = sortCategories()
            allCreditCards = creditCardRepository.findAll()

            reset()

            loadedTransaction = screen.initialTransactionId?.let {
                transactionRepo.findById(TransactionId(it))?.toDraft()
            } ?: newDraft(screen, getAccounts.pickable().ifEmpty { getAccounts })

            accounts = getAccounts.pickable(
                listOf(
                    AccountId(loadedTransaction().accountId),
                    loadedTransaction().toAccountId?.let(::AccountId),
                )
            ).toImmutableList()

            tags = tagList.await()
            transactionAssociatedTags =
                tagRepository.findByAssociatedId(AssociationId(loadedTransaction().id)).map(Tag::id)
                    .toImmutableList()
            display(loadedTransaction!!)
        }
    }

    @Composable
    override fun uiState(): EditTransactionViewState {
        return EditTransactionViewState(
            transactionType = transactionType,
            initialTitle = initialTitle,
            titleSuggestions = getTitleSuggestions(),
            currency = currency,
            description = description,
            dateTime = dateTime,
            dueDate = dueDate,
            creditCards = accountCreditCards(),
            creditCard = creditCard,
            chargeDate = chargeDate(),
            canMoveChargeEarlier = canMoveChargeEarlier(),
            accounts = accounts,
            categories = categories,
            account = account,
            toAccount = toAccount,
            category = category,
            amount = amount,
            hasChanges = hasChanges,
            displayLoanHelper = displayLoanHelper,
            backgroundProcessingStarted = backgroundProcessingStarted,
            customExchangeRateState = customExchangeRateState,
            tags = tags,
            transactionAssociatedTags = transactionAssociatedTags
        )
    }

    /** The cards of the account an expense can be paid with. */
    private fun accountCreditCards(): ImmutableList<CreditCard> =
        if (transactionType == TransactionType.EXPENSE) {
            allCreditCards.filter { it.account == account?.id }.toImmutableList()
        } else {
            persistentListOf()
        }

    /** When the card charges the expense, if it's paid with a credit card and not planned. */
    private fun chargeDate(): LocalDateTime? {
        if (transactionType != TransactionType.EXPENSE || dueDate != null) return null
        return creditCard?.chargeDateTime(purchase(), chargeShift)
    }

    private fun canMoveChargeEarlier(): Boolean {
        val card = creditCard ?: return false
        return chargeShift > card.minChargeShift(purchase())
    }

    private fun purchase(): LocalDateTime = with(timeConverter) {
        (dateTime ?: timeProvider.utcNow()).toLocalDateTime()
    }

    @Composable
    private fun getTitleSuggestions(): ImmutableSet<String> {
        return if (features.showTitleSuggestions.asEnabledState()) {
            titleSuggestions
        } else {
            persistentSetOf()
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun onEvent(event: EditTransactionViewEvent) {
        when (event) {
            is EditTransactionViewEvent.CreateAccount -> createAccount(event.data)
            is EditTransactionViewEvent.CreateCategory -> createCategory(event.data)
            EditTransactionViewEvent.Delete -> delete()
            EditTransactionViewEvent.Duplicate -> duplicate()
            is EditTransactionViewEvent.EditCategory -> editCategory(event.updatedCategory)
            is EditTransactionViewEvent.OnAccountChanged -> onAccountChanged(event.newAccount)
            is EditTransactionViewEvent.OnAmountChanged -> onAmountChanged(event.newAmount)
            is EditTransactionViewEvent.OnCategoryChanged -> onCategoryChanged(event.newCategory)
            is EditTransactionViewEvent.OnDescriptionChanged ->
                onDescriptionChanged(event.newDescription)

            is EditTransactionViewEvent.OnDueDateChanged -> onDueDateChanged(event.newDueDate)
            EditTransactionViewEvent.OnPayPlannedPayment -> onPayPlannedPayment()
            is EditTransactionViewEvent.OnChangeDate -> handleChangeDate()
            is EditTransactionViewEvent.OnChangeTime -> handleChangeTime()
            is EditTransactionViewEvent.OnMoveCharge -> onMoveCharge(event.months)
            is EditTransactionViewEvent.OnCreditCardChanged -> onCreditCardChanged(event.creditCard)
            is EditTransactionViewEvent.OnSetTransactionType ->
                onSetTransactionType(event.newTransactionType)

            is EditTransactionViewEvent.OnTitleChanged -> onTitleChanged(event.newTitle)
            is EditTransactionViewEvent.OnToAccountChanged -> onToAccountChanged(event.newAccount)
            is EditTransactionViewEvent.Save -> save(event.closeScreen)
            is EditTransactionViewEvent.SetHasChanges -> setHasChanges(event.hasChangesValue)
            is EditTransactionViewEvent.UpdateExchangeRate -> updateExchangeRate(event.exRate)
            is EditTransactionViewEvent.TagEvent -> handleTagEvent(event)
        }
    }

    private fun handleTagEvent(event: EditTransactionViewEvent.TagEvent) {
        when (event) {
            is EditTransactionViewEvent.TagEvent.SaveTag -> onTagSaved(event.name)
            is EditTransactionViewEvent.TagEvent.OnTagSelect -> associateTagToTransaction(event.selectedTag)
            is EditTransactionViewEvent.TagEvent.OnTagDeSelect -> removeTagAssociation(event.selectedTag)
            is EditTransactionViewEvent.TagEvent.OnTagSearch -> searchTag(event.query)
            is EditTransactionViewEvent.TagEvent.OnTagDelete -> deleteTag(event.selectedTag)
            is EditTransactionViewEvent.TagEvent.OnTagEdit -> updateTagInformation(event.newTag)
        }
    }

    private suspend fun defaultAccountId(
        screen: EditTransactionScreen,
        accounts: List<Account>,
    ): UUID {
        if (screen.accountId != null) {
            return screen.accountId!!
        }

        val lastSelectedId = appPreferences.lastSelectedAccountId()?.let { UUID.fromString(it) }
        if (lastSelectedId != null && ioThread {
                accounts.find {
                    it.id.value == lastSelectedId
                }
            } != null
        ) {
            // use last selected account
            return lastSelectedId
        }

        return accounts.first().id.value
    }

    private suspend fun display(transaction: TransactionDraft) {
        this.title = transaction.title

        transactionType = transaction.type
        initialTitle = transaction.title
        dateTime = transaction.dateTime
        description = transaction.description
        dueDate = transaction.dueDate
        chargeShift = transaction.chargeShift
        creditCard = allCreditCards.find { it.id.value == transaction.creditCardId }
        paidHistory = transaction.paidFor
        val selectedAccount = accountRepository.findById(AccountId(transaction.accountId))!!
        account = selectedAccount
        toAccount = transaction.toAccountId?.let {
            accountRepository.findById(AccountId(it))
        }
        category = transaction.categoryId?.let {
            categoryRepository.findById(CategoryId(it))
        }
        amount = transaction.amount

        updateCurrency(account = selectedAccount)

        customExchangeRateState = if (transaction.toAccountId == null) {
            CustomExchangeRateState()
        } else {
            val exchangeRate = transaction.toAmount / transaction.amount
            val toAccountCurrency =
                accounts.find { acc -> acc.id.value == transaction.toAccountId }?.asset?.code
            CustomExchangeRateState(
                showCard = toAccountCurrency != account?.asset?.code,
                exchangeRate = exchangeRate,
                convertedAmount = transaction.toAmount,
                toCurrencyCode = toAccountCurrency,
                fromCurrencyCode = currency
            )
        }

        displayLoanHelper = getDisplayLoanHelper(trans = transaction)
    }

    private suspend fun getDisplayLoanHelper(trans: TransactionDraft): EditTransactionDisplayLoan {
        if (trans.loanId == null) {
            return EditTransactionDisplayLoan()
        }

        val loan =
            ioThread { loanDao.findById(trans.loanId) } ?: return EditTransactionDisplayLoan()
        val isLoanRecord = trans.loanRecordId != null

        val loanWarningDescription = if (isLoanRecord) {
            context.getString(
                R.string.note_transaction_associated_with_loan_record_of_loan,
                loan.name
            )
        } else {
            context.getString(
                R.string.note_you_are_trying_to_change_the_account_associated_with_the_loan,
                loan.name
            )
        }

        val loanCaption =
            if (isLoanRecord) {
                context.getString(
                    R.string.this_transaction_is_associated_with_loan_record,
                    loan.name
                )
            } else {
                context.getString(
                    R.string.this_transaction_is_associated_with_loan,
                    loan.name
                )
            }

        return EditTransactionDisplayLoan(
            isLoan = true,
            isLoanRecord = isLoanRecord,
            loanCaption = loanCaption,
            loanWarningDescription = loanWarningDescription
        )
    }

    private fun onAmountChanged(newAmount: Double) {
        viewModelScope.launch {
            loadedTransaction = loadedTransaction().copy(
                amount = newAmount
            )
            amount = newAmount
            updateCustomExchangeRateState(amt = newAmount)

            saveIfEditMode()
        }
    }

    private fun onTitleChanged(newTitle: String?) {
        loadedTransaction = loadedTransaction().copy(
            title = newTitle
        )
        this.title = newTitle

        saveIfEditMode()

        updateTitleSuggestions(newTitle)
    }

    private fun onDescriptionChanged(newDescription: String?) {
        loadedTransaction = loadedTransaction().copy(
            description = newDescription
        )
        description = newDescription

        saveIfEditMode()
    }

    private fun onAccountChanged(newAccount: Account) {
        viewModelScope.launch {
            // Another account's card can't pay it: the one its new expenses start on instead.
            if (newAccount.id != account?.id) {
                creditCard = defaultCreditCard(newAccount, transactionType)
                chargeShift = 0
            }
            loadedTransaction = loadedTransaction().copy(
                accountId = newAccount.id.value,
                creditCardId = creditCard?.id?.value,
                chargeShift = chargeShift,
            )
            account = newAccount

            updateCustomExchangeRateState(fromAccount = newAccount)

            viewModelScope.launch {
                updateCurrency(account = newAccount)
            }

            // update last selected account
            appPreferences.setLastSelectedAccountId(newAccount.id.value.toString())

            saveIfEditMode()

            updateTitleSuggestions()
        }
    }

    private fun updateCurrency(account: Account) {
        currency = account.asset.code
    }

    private fun onToAccountChanged(newAccount: Account) {
        viewModelScope.launch {
            loadedTransaction = loadedTransaction().copy(
                toAccountId = newAccount.id.value
            )
            toAccount = newAccount
            updateCustomExchangeRateState(toAccountValue = newAccount)

            saveIfEditMode()
        }
    }

    private fun onDueDateChanged(newDueDate: LocalDateTime?) {
        val newDueDateUtc = with(timeConverter) { newDueDate?.toUTC() }
        loadedTransaction = loadedTransaction().copy(
            dueDate = newDueDateUtc
        )
        dueDate = newDueDateUtc

        saveIfEditMode()
    }

    private fun handleChangeDate() {
        dateTimePicker.pickDate(
            initialDate = loadedTransaction?.dateTime,
        ) { localDate ->
            val localTime = loadedTransaction().dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()
            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleChangeTime() {
        dateTimePicker.pickTime(
            initialTime = loadedTransaction?.dateTime?.let {
                with(timeConverter) {
                    it.toLocalDateTime()
                }
            }?.toLocalTime()
        ) { localTime ->
            val localDate = loadedTransaction().dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: timeProvider.localDateNow()
            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun updateDateTime(newDateTime: LocalDateTime) {
        val newDateTimeUtc = with(timeConverter) { newDateTime.toUTC() }
        loadedTransaction = loadedTransaction().copy(
            dateTime = newDateTimeUtc,
        )
        dateTime = newDateTimeUtc

        saveIfEditMode()
    }

    // The move is kept when the purchase date changes; a move earlier than the purchase allows
    // counts as the earliest one.
    private fun onMoveCharge(months: Int) {
        val earliest = creditCard?.minChargeShift(purchase()) ?: return
        chargeShift = (chargeShift.coerceAtLeast(earliest) + months).coerceAtLeast(earliest)
        loadedTransaction = loadedTransaction().copy(chargeShift = chargeShift)

        saveIfEditMode()
    }

    private fun onSetTransactionType(newTransactionType: TransactionType) {
        // Only an expense is paid with a card.
        if (newTransactionType != transactionType) {
            creditCard = account?.let { defaultCreditCard(it, newTransactionType) }
            chargeShift = 0
        }
        loadedTransaction = loadedTransaction().copy(
            type = newTransactionType,
            creditCardId = creditCard?.id?.value,
            chargeShift = chargeShift,
        )
        transactionType = newTransactionType
        saveIfEditMode()
    }

    // A move counts from a card's usual charge date, so another card starts without one.
    private fun onCreditCardChanged(newCreditCard: CreditCard?) {
        creditCard = newCreditCard
        chargeShift = 0
        loadedTransaction = loadedTransaction().copy(
            creditCardId = newCreditCard?.id?.value,
            chargeShift = 0,
        )

        saveIfEditMode()
    }

    private suspend fun newDraft(screen: EditTransactionScreen, accounts: List<Account>): TransactionDraft {
        val accountId = defaultAccountId(screen = screen, accounts = accounts)
        val card = accounts.find { it.id.value == accountId }?.let { defaultCreditCard(it, screen.type) }
        return TransactionDraft(
            accountId = accountId,
            categoryId = screen.categoryId,
            type = screen.type,
            amount = 0.0,
            toAmount = 0.0,
            creditCardId = card?.id?.value,
        )
    }

    /** What a new expense on [account] is paid with: a credit card account's own card. */
    private fun defaultCreditCard(account: Account, type: TransactionType): CreditCard? =
        allCreditCards.firstOrNull { it.account == account.id }
            ?.takeIf { type == TransactionType.EXPENSE && account.type == AccountType.CREDIT_CARD }

    private fun onPayPlannedPayment() {
        viewModelScope.launch {
            val paidTransaction = loadedTransaction().toDomain(transactionMapper)
                ?.let { dueTransactionsUseCase.pay(it) }
                ?.toDraft()
                ?: return@launch
            loadedTransaction = paidTransaction
            paidHistory = paidTransaction.paidFor
            dueDate = paidTransaction.dueDate
            dateTime = paidTransaction.dateTime

            saveIfEditMode(
                closeScreen = true
            )
        }
    }

    private fun delete() {
        viewModelScope.launch {
            ioThread {
                loadedTransaction?.let {
                    transactionRepo.deleteById(TransactionId(it.id))
                }
                closeScreen()
            }
        }
    }

    private fun duplicate() {
        viewModelScope.launch {
            ioThread {
                val id = UUID.randomUUID()
                loadedTransaction()
                    .copy(
                        id = id,
                        dateTime = timeProvider.utcNow(),
                        // Not another payment of the planned rule, nor part of the loan.
                        dueDate = null,
                        recurringRuleId = null,
                        paidFor = null,
                        loanId = null,
                        loanRecordId = null,
                    )
                    .toDomain(transactionMapper)
                    ?.let {
                        transactionRepo.save(creditCardChargeUseCase.dated(it))
                    }

                tagRepository.findByIds(transactionAssociatedTags).forEach {
                    associateTagToTransaction(it, id)
                }

                context.refreshWidget(WalletBalanceWidgetReceiver::class.java)
            }

            closeScreen()
        }
    }

    private fun createCategory(data: CreateCategoryData) {
        viewModelScope.launch {
            categoryCreator.createCategory(data) {
                categories = sortCategories()

                // Select the newly created category
                onCategoryChanged(it)
            }
        }
    }

    private fun onCategoryChanged(newCategory: Category?) {
        loadedTransaction = loadedTransaction().copy(
            categoryId = newCategory?.id?.value
        )
        category = newCategory

        saveIfEditMode()

        updateTitleSuggestions()
    }

    private var titleSuggestionsJob: Job? = null

    private fun updateTitleSuggestions(title: String? = loadedTransaction().title) {
        // Each keystroke asks again: an older, slower lookup must not overwrite a newer one.
        titleSuggestionsJob?.cancel()
        titleSuggestionsJob = viewModelScope.launch {
            titleSuggestions = ioThread {
                smartTitleSuggestionsLogic.suggest(
                    title = title,
                    categoryId = category?.id?.value,
                    accountId = account?.id?.value
                )
            }.toPersistentSet()
        }
    }

    private fun editCategory(updatedCategory: Category) {
        viewModelScope.launch {
            categoryCreator.editCategory(updatedCategory) {
                categories = sortCategories()
            }
        }
    }

    private fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            accountCreator.createAccount(data) {
                accounts = accountsAct().pickable(listOf(account?.id, toAccount?.id)).toImmutableList()
                allCreditCards = creditCardRepository.findAll()
            }
        }
    }

    private fun save(closeScreen: Boolean = true) {
        if (!validTransaction()) {
            return
        }
        // A double tap would save twice and go back twice.
        if (closeScreen && closingSave?.isActive == true) return

        val job = viewModelScope.launch {
            saveInternal(closeScreen = closeScreen)
        }
        if (closeScreen) closingSave = job
    }

    private var closingSave: Job? = null

    private suspend fun saveInternal(closeScreen: Boolean) {
        try {
            ioThread {
                loadedTransaction = loadedTransaction().copy(
                    accountId = account?.id?.value ?: error("no accountId"),
                    toAccountId = toAccount?.id?.value,
                    toAmount = customExchangeRateState.convertedAmount ?: amount,
                    title = title?.trim(),
                    description = description?.trim(),
                    amount = amount,
                    type = transactionType,
                    dueDate = dueDate,
                    paidFor = paidHistory,
                    dateTime = when {
                        loadedTransaction().dateTime == null &&
                                dueDate == null -> {
                            timeProvider.utcNow()
                        }

                        else -> loadedTransaction().dateTime
                    },
                    categoryId = category?.id?.value,
                )

                loadedTransaction().toDomain(transactionMapper)?.let {
                    if (it.metadata.loanId != null) {
                        backgroundProcessingStarted = true
                        loanTransactionsUseCase.updateFromTransaction(it)
                        backgroundProcessingStarted = false
                    }
                    transactionRepo.save(creditCardChargeUseCase.dated(it))
                }

                context.refreshWidget(WalletBalanceWidgetReceiver::class.java)
            }

            if (closeScreen) {
                closeScreen()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    @JvmName("setHasChangesMethod")
    private fun setHasChanges(hasChangesValue: Boolean) {
        hasChanges = hasChangesValue
    }

    private suspend fun baseCurrency(): String = ioThread { currencyRepository.getBaseCurrency().code }

    private fun closeScreen() {
        if (nav.backStackEmpty()) {
            nav.resetBackStack()
            nav.navigateTo(MainScreen)
        } else {
            nav.back()
        }
    }

    // Comment for push
    @Suppress("ReturnCount")
    private fun validTransaction(): Boolean {
        if (hasChosenSameSourceAndDestinationAccountToTransfer()) {
            viewModelScope.launch {
                toaster.show(R.string.msg_source_account_destination_account_same_for_transfer)
            }
            return false
        }
        if (hasNotChosenAccountToTransfer()) {
            viewModelScope.launch {
                toaster.show(R.string.msg_select_account_to_transfer)
            }
            return false
        }

        if (amount <= 0.0) {
            return false
        }
        if (transactionType == TransactionType.TRANSFER && customExchangeRateState.showCard &&
            customExchangeRateState.exchangeRate == null
        ) {
            viewModelScope.launch {
                toaster.show(R.string.msg_enter_exchange_rate)
            }
            return false
        }

        return true
    }

    private fun hasNotChosenAccountToTransfer(): Boolean {
        return transactionType == TransactionType.TRANSFER && toAccount == null
    }

    private fun hasChosenSameSourceAndDestinationAccountToTransfer(): Boolean {
        return transactionType == TransactionType.TRANSFER && toAccount == account
    }

    private fun reset() {
        loadedTransaction = null

        initialTitle = null
        description = null
        dueDate = null
        category = null
        hasChanges = false
    }

    private fun loadedTransaction() = loadedTransaction ?: error("Loaded transaction is null")

    private fun updateExchangeRate(exRate: Double?) {
        // With a rate of 0 nothing is received, and the transfer would be stored with the sent
        // amount on the receiving side instead.
        if (exRate != null && exRate <= 0.0) return
        viewModelScope.launch {
            updateCustomExchangeRateState(exchangeRate = exRate, resetRate = exRate == null)
        }
    }

    private suspend fun updateCustomExchangeRateState(
        toAccountValue: Account? = null,
        fromAccount: Account? = null,
        amt: Double? = null,
        exchangeRate: Double? = null,
        resetRate: Boolean = false
    ) {
        computationThread {
            val toAcc = toAccountValue ?: toAccount
            val fromAcc = fromAccount ?: account

            val toAccCurrencyCode = toAcc?.asset?.code ?: baseUserCurrency
            val fromAccCurrencyCode = fromAcc?.asset?.code ?: baseUserCurrency

            if (toAcc == null || fromAcc == null || (toAccCurrencyCode == fromAccCurrencyCode)) {
                customExchangeRateState = CustomExchangeRateState()
                return@computationThread
            }

            val exRate = exchangeRate
                ?: if (isCustomExchangeRateCurrencyCodeMatchingWithSourceAndDestinationAccountCurrencyCode(
                        toAccCurrencyCode = toAccCurrencyCode,
                        fromAccCurrencyCode = fromAccCurrencyCode
                    ) && !resetRate
                ) {
                    customExchangeRateState.exchangeRate
                } else {
                    exchangeUseCase.convert(
                        PositiveValue(PositiveDouble.unsafe(1.0), fromAcc.asset),
                        to = toAcc.asset
                    ).getOrNull()?.amount?.value
                }

            val amount = amt ?: amount

            val customTransferExchangeRateState = CustomExchangeRateState(
                showCard = true,
                toCurrencyCode = toAccCurrencyCode,
                fromCurrencyCode = fromAccCurrencyCode,
                exchangeRate = exRate,
                convertedAmount = exRate?.let { it * amount }
            )

            customExchangeRateState = customTransferExchangeRateState
            uiThread {
                saveIfEditMode()
            }
        }
    }

    private fun isCustomExchangeRateCurrencyCodeMatchingWithSourceAndDestinationAccountCurrencyCode(
        toAccCurrencyCode: String,
        fromAccCurrencyCode: String
    ): Boolean {
        return customExchangeRateState.showCard &&
                toAccCurrencyCode == customExchangeRateState.toCurrencyCode &&
                fromAccCurrencyCode == customExchangeRateState.fromCurrencyCode
    }

    private fun saveIfEditMode(closeScreen: Boolean = false) {
        if (editMode) {
            hasChanges = true

            save(closeScreen)
        }
    }

    private suspend fun getAllTags(): ImmutableList<Tag> =
        tagRepository.findAll().toImmutableList()

    private fun onTagSaved(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            NotBlankTrimmedString.from(name.toLowerCaseLocal())
                .onRight {
                    val tag = with(tagMapper) { createNewTag(name = it) }
                    tagRepository.save(tag)
                    this@EditTransactionViewModel.tags = getAllTags()
                }

            saveIfEditMode()
        }
    }

    private fun associateTagToTransaction(selectedTag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            val associatedId = AssociationId(loadedTransaction().id)
            tagRepository.associateTagToEntity(associatedId, selectedTag.id)
            transactionAssociatedTags =
                tagRepository.findByAssociatedId(associatedId).map(Tag::id).toImmutableList()
        }
    }

    private fun associateTagToTransaction(selectedTag: Tag, id: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            val associatedId = AssociationId(id)
            tagRepository.associateTagToEntity(associatedId, selectedTag.id)
            transactionAssociatedTags =
                tagRepository.findByAssociatedId(associatedId).map(Tag::id).toImmutableList()
        }
    }

    private fun removeTagAssociation(selectedTag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            val associatedId = AssociationId(loadedTransaction().id)
            tagRepository.removeTagAssociation(associatedId, selectedTag.id)
            transactionAssociatedTags =
                tagRepository.findByAssociatedId(associatedId).map(Tag::id).toImmutableList()
        }
    }

    private fun searchTag(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagSearchJob?.cancelAndJoin()
            delay(tagSearchDebounceTimeInMills) // Debounce effect
            tagSearchJob = launch(Dispatchers.IO) {
                NotBlankTrimmedString.from(query.toLowerCaseLocal())
                    .onRight {
                        tags =
                            tagRepository.findByText(text = it.value).toImmutableList()
                    }
                    .onLeft {
                        tags = tagRepository.findAll().toImmutableList()
                    }
            }
        }
    }

    private fun deleteTag(selectedTag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.deleteById(selectedTag.id)
            tags = tagRepository.findAll().toImmutableList()
        }
    }

    private fun updateTagInformation(newTag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.save(newTag)
            tags = tagRepository.findAll().toImmutableList()
        }
    }

    private suspend fun sortCategories(): ImmutableList<Category> {
        val categories = categoryRepository.findAll()
        return if (shouldSortCategoriesAscending()) {
            categories.sortedBy { it.name.value }.toImmutableList()
        } else {
            categories.toImmutableList()
        }
    }

    private suspend fun shouldSortCategoriesAscending(): Boolean {
        return features.sortCategoriesAscending.isEnabled(context)
    }
}
