package com.ivy.transaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CreditCard
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.TransactionId
import com.ivy.data.model.pickable
import com.ivy.data.model.primitive.AssociationId
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.creditcard.minChargeShift
import com.ivy.domain.features.Features
import com.ivy.wallet.data.EditTransactionDisplayLoan
import com.ivy.wallet.domain.logic.AccountCreator
import com.ivy.wallet.utils.computationThread
import com.ivy.wallet.utils.ioThread
import com.ivy.wallet.utils.uiThread
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.data.CustomExchangeRateState
import com.ivy.wallet.domain.logic.CategoryCreator
import com.ivy.wallet.domain.logic.SmartTitleSuggestionsLogic
import com.ivy.wallet.domain.logic.model.CreateAccountData
import com.ivy.wallet.domain.logic.model.CreateCategoryData
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Stable
@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    private val transactionLoanDisplay: TransactionLoanDisplay,
    private val transferExchangeRate: TransferExchangeRate,
    private val transactionSaving: TransactionSaving,
    private val nav: Navigation,
    private val appPreferences: AppPreferences,
    private val categoryCreator: CategoryCreator,
    private val accountCreator: AccountCreator,
    private val smartTitleSuggestionsLogic: SmartTitleSuggestionsLogic,
    private val sortedCategories: SortedCategories,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepo: TransactionRepository,
    private val transactionTags: TransactionTags,
    private val features: Features,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val transactionDateTimePicker: TransactionDateTimePicker,
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

            baseUserCurrency = transferExchangeRate.baseCurrency()

            val tagList = async { transactionTags.getAllTags() }

            val getAccounts = accountRepository.findAll()
            if (getAccounts.isEmpty()) {
                closeScreen()
                return@launch
            }

            categories = sortedCategories.sortCategories()
            allCreditCards = creditCardRepository.findAll()

            reset()

            loadedTransaction = screen.initialTransactionId?.let {
                transactionRepo.findById(TransactionId(it))?.toDraft()
            } ?: newDraft(screen, getAccounts.pickable().ifEmpty { getAccounts }, allCreditCards, appPreferences)

            accounts = getAccounts.pickable(
                listOf(
                    AccountId(loadedTransaction().accountId),
                    loadedTransaction().toAccountId?.let(::AccountId),
                )
            ).toImmutableList()

            tags = tagList.await()
            transactionAssociatedTags = transactionTags.associatedTagIds(AssociationId(loadedTransaction().id))
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
            creditCards = accountCreditCards(transactionType, allCreditCards, account),
            creditCard = creditCard,
            chargeDate = chargeDate(transactionType, dueDate, creditCard, chargeShift, ::purchase),
            canMoveChargeEarlier = canMoveChargeEarlier(creditCard, chargeShift, ::purchase),
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

        customExchangeRateState = displayedExchangeRateState(transaction, accounts, account, currency)

        displayLoanHelper = transactionLoanDisplay.getDisplayLoanHelper(trans = transaction)
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
                creditCard = defaultCreditCard(allCreditCards, newAccount, transactionType)
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
        transactionDateTimePicker.pickDate(
            initialDate = loadedTransaction?.dateTime,
            dateTime = { loadedTransaction().dateTime },
            onPicked = ::updateDateTime,
        )
    }

    private fun handleChangeTime() {
        transactionDateTimePicker.pickTime(
            initialDateTime = loadedTransaction?.dateTime,
            dateTime = { loadedTransaction().dateTime },
            onPicked = ::updateDateTime,
        )
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
            creditCard = account?.let { defaultCreditCard(allCreditCards, it, newTransactionType) }
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

    private fun onPayPlannedPayment() {
        viewModelScope.launch {
            val paidTransaction = transactionSaving.pay(loadedTransaction()) ?: return@launch
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
            val saved = transactionSaving.trySaving {
                val id = UUID.randomUUID()
                transactionSaving.saveCopy(loadedTransaction(), id) {
                    transactionTags.findByIds(transactionAssociatedTags).forEach {
                        associateTagToTransaction(it, id)
                    }
                }
            }

            if (saved) closeScreen()
        }
    }

    private fun createCategory(data: CreateCategoryData) {
        viewModelScope.launch {
            categoryCreator.createCategory(data) {
                categories = sortedCategories.sortCategories()

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
                categories = sortedCategories.sortCategories()
            }
        }
    }

    private fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            accountCreator.createAccount(data) {
                accounts = accountRepository.findAll().pickable(listOf(account?.id, toAccount?.id)).toImmutableList()
                allCreditCards = creditCardRepository.findAll()
            }
        }
    }

    private fun save(closeScreen: Boolean = true) {
        val valid = transactionSaving.validTransaction(
            scope = viewModelScope,
            transactionType = transactionType,
            account = account,
            toAccount = toAccount,
            amount = amount,
            customExchangeRateState = customExchangeRateState,
        )
        if (!valid) {
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
        val saved = transactionSaving.trySaving {
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

            transactionSaving.save(loadedTransaction()) { backgroundProcessingStarted = it }
        }

        if (saved && closeScreen) {
            closeScreen()
        }
    }

    @JvmName("setHasChangesMethod")
    private fun setHasChanges(hasChangesValue: Boolean) {
        hasChanges = hasChangesValue
    }

    private fun closeScreen() {
        if (nav.backStackEmpty()) {
            nav.resetBackStack()
            nav.navigateTo(MainScreen)
        } else {
            nav.back()
        }
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
            val customTransferExchangeRateState = transferExchangeRate.exchangeRateState(
                customExchangeRateState = customExchangeRateState,
                toAcc = toAccountValue ?: toAccount,
                fromAcc = fromAccount ?: account,
                baseUserCurrency = { baseUserCurrency },
                exchangeRate = exchangeRate,
                resetRate = resetRate,
            ) { amt ?: amount }
            if (customTransferExchangeRateState == null) {
                customExchangeRateState = CustomExchangeRateState()
                return@computationThread
            }

            customExchangeRateState = customTransferExchangeRateState
            uiThread {
                saveIfEditMode()
            }
        }
    }

    private fun saveIfEditMode(closeScreen: Boolean = false) {
        if (editMode) {
            hasChanges = true

            save(closeScreen)
        }
    }

    private fun onTagSaved(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionTags.saveTag(name)?.let { this@EditTransactionViewModel.tags = it }

            saveIfEditMode()
        }
    }

    private fun associateTagToTransaction(selectedTag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            val associatedId = AssociationId(loadedTransaction().id)
            transactionAssociatedTags = transactionTags.associateTag(associatedId, selectedTag)
        }
    }

    private fun associateTagToTransaction(selectedTag: Tag, id: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            val associatedId = AssociationId(id)
            transactionAssociatedTags = transactionTags.associateTag(associatedId, selectedTag)
        }
    }

    private fun removeTagAssociation(selectedTag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            val associatedId = AssociationId(loadedTransaction().id)
            transactionAssociatedTags = transactionTags.removeTagAssociation(associatedId, selectedTag)
        }
    }

    private fun searchTag(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagSearchJob?.cancelAndJoin()
            delay(tagSearchDebounceTimeInMills) // Debounce effect
            tagSearchJob = launch(Dispatchers.IO) {
                tags = transactionTags.searchTags(query)
            }
        }
    }

    private fun deleteTag(selectedTag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            tags = transactionTags.deleteTag(selectedTag)
        }
    }

    private fun updateTagInformation(newTag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            tags = transactionTags.updateTag(newTag)
        }
    }
}
