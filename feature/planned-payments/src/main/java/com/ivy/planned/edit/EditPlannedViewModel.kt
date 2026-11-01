package com.ivy.planned.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.base.model.IntervalType
import com.ivy.data.model.PlannedPaymentInterval
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PlannedPaymentRuleId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.primitive.PositiveInt
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.PlannedPaymentRuleRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.planned.PlannedPaymentsGenerator
import com.ivy.wallet.domain.logic.AccountCreator
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.logic.CategoryCreator
import com.ivy.wallet.domain.logic.model.CreateAccountData
import com.ivy.wallet.domain.logic.model.CreateCategoryData
import com.ivy.wallet.ui.theme.modal.RecurringRuleModalData
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import javax.inject.Inject

@Stable
@HiltViewModel
class EditPlannedViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val nav: Navigation,
    private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
    private val plannedPaymentsGenerator: PlannedPaymentsGenerator,
    private val categoryCreator: CategoryCreator,
    private val accountCreator: AccountCreator,
    private val accountsAct: AccountsAct,
    private val transactionRepository: TransactionRepository,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<EditPlannedScreenState, EditPlannedScreenEvent>() {

    private val saveMutex = Mutex()

    private var transactionType by mutableStateOf(TransactionType.INCOME)
    private var startDate by mutableStateOf<LocalDateTime?>(null)
    private var intervalN by mutableStateOf<Int?>(null)
    private var intervalType by mutableStateOf<IntervalType?>(null)
    private var oneTime by mutableStateOf(false)
    private var initialTitle by mutableStateOf<String?>(null)
    private var description by mutableStateOf<String?>(null)
    private var account by mutableStateOf<Account?>(null)
    private var category by mutableStateOf<Category?>(null)
    private var amount by mutableDoubleStateOf(0.0)
    private var currency by mutableStateOf("")
    private var categories by mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var categoryModalVisible by mutableStateOf(false)
    private var descriptionModalVisible by mutableStateOf(false)
    private var deleteTransactionModalVisible by mutableStateOf(false)
    private var transactionTypeModalVisible by mutableStateOf(false)
    private var amountModalVisible by mutableStateOf(false)
    private var recurringRuleModalData by mutableStateOf<RecurringRuleModalData?>(null)
    private var categoryModalData by mutableStateOf<CategoryModalData?>(null)
    private var accountModalData by mutableStateOf<AccountModalData?>(null)

    private var ruleId: PlannedPaymentRuleId? = null
    private var editMode = false
    private var title: String? = null

    @Composable
    override fun uiState(): EditPlannedScreenState {
        return EditPlannedScreenState(
            currency = currency,
            categories = categories,
            accounts = accounts,
            transactionType = transactionType,
            startDate = startDate,
            intervalN = intervalN,
            oneTime = oneTime,
            account = account,
            category = category,
            amount = amount,
            initialTitle = initialTitle,
            description = description,
            intervalType = intervalType,
            categoryModalVisible = categoryModalVisible,
            categoryModalData = categoryModalData,
            accountModalData = accountModalData,
            deleteTransactionModalVisible = deleteTransactionModalVisible,
            descriptionModalVisible = descriptionModalVisible,
            amountModalVisible = amountModalVisible,
            transactionTypeModalVisible = transactionTypeModalVisible,
            recurringRuleModalData = recurringRuleModalData
        )
    }

    override fun onEvent(event: EditPlannedScreenEvent) {
        when (event) {
            is EditPlannedScreenEvent.OnSave -> save()
            is EditPlannedScreenEvent.OnDelete -> delete()
            is EditPlannedScreenEvent.OnSetTransactionType ->
                updateTransactionType(event.newTransactionType)

            is EditPlannedScreenEvent.OnDescriptionChanged ->
                updateDescription(event.newDescription)

            is EditPlannedScreenEvent.OnCreateAccount -> createAccount(event.data)
            is EditPlannedScreenEvent.OnCreateCategory -> createCategory(event.data)
            is EditPlannedScreenEvent.OnAccountChanged -> updateAccount(event.newAccount)
            is EditPlannedScreenEvent.OnAmountChanged -> updateAmount(event.newAmount)
            is EditPlannedScreenEvent.OnTitleChanged -> updateTitle(event.newTitle)
            is EditPlannedScreenEvent.OnRuleChanged ->
                updateRule(event.startDate, event.oneTime, event.intervalN, event.intervalType)

            is EditPlannedScreenEvent.OnCategoryChanged -> updateCategory(event.newCategory)
            is EditPlannedScreenEvent.OnEditCategory -> editCategory(event.updatedCategory)
            is EditPlannedScreenEvent.OnCategoryModalVisible ->
                categoryModalVisible = event.visible

            is EditPlannedScreenEvent.OnCategoryModalDataChanged ->
                categoryModalData = event.categoryModalData

            is EditPlannedScreenEvent.OnAccountModalDataChanged ->
                accountModalData = event.accountModalData

            is EditPlannedScreenEvent.OnDescriptionModalVisible ->
                descriptionModalVisible = event.visible

            is EditPlannedScreenEvent.OnTransactionTypeModalVisible ->
                transactionTypeModalVisible = event.visible

            is EditPlannedScreenEvent.OnAmountModalVisible ->
                amountModalVisible = event.visible

            is EditPlannedScreenEvent.OnDeleteTransactionModalVisible ->
                deleteTransactionModalVisible = event.visible

            is EditPlannedScreenEvent.OnRecurringRuleModalDataChanged ->
                recurringRuleModalData = event.recurringRuleModalData
        }
    }

    fun start(screen: EditPlannedScreen) {
        viewModelScope.launch {
            transactionType = screen.type
            editMode = screen.plannedPaymentRuleId != null

            val accounts = accountsAct()
            if (accounts.isEmpty()) {
                nav.back()
                return@launch
            }
            this@EditPlannedViewModel.accounts = accounts
            categories = categoryRepository.findAll().toImmutableList()

            reset()

            val rule = screen.plannedPaymentRuleId?.let {
                plannedPaymentRuleRepository.findById(PlannedPaymentRuleId(it))
            }
            if (rule != null) {
                display(rule)
            } else {
                displayNew(screen, accounts)
            }
        }
    }

    private suspend fun display(rule: PlannedPaymentRule) {
        ruleId = rule.id
        title = rule.title?.value
        initialTitle = rule.title?.value
        description = rule.description?.value
        transactionType = rule.type
        startDate = with(timeConverter) { rule.startDate.toLocalDateTime() }
        oneTime = rule.interval == null
        intervalN = rule.interval?.n?.value
        intervalType = rule.interval?.type
        account = accountRepository.findById(rule.account)
        category = rule.category?.let { categoryRepository.findById(it) }
        amount = rule.amount.value

        account?.let(::updateCurrency)
    }

    private suspend fun displayNew(screen: EditPlannedScreen, accounts: List<Account>) {
        ruleId = PlannedPaymentRuleId(UUID.randomUUID())
        title = screen.title
        initialTitle = screen.title
        description = screen.description
        startDate = null
        oneTime = false
        intervalN = null
        intervalType = null
        account = accounts.find { it.id.value == screen.accountId } ?: accounts.first()
        category = screen.categoryId?.let { categoryRepository.findById(CategoryId(it)) }
        amount = screen.amount ?: 0.0

        account?.let(::updateCurrency)
    }

    private fun updateCurrency(account: Account) {
        currency = account.asset.code
    }

    private fun updateRule(
        startDate: LocalDateTime,
        oneTime: Boolean,
        intervalN: Int?,
        intervalType: IntervalType?
    ) {
        this@EditPlannedViewModel.startDate = startDate
        this@EditPlannedViewModel.intervalN = intervalN
        this@EditPlannedViewModel.intervalType = intervalType
        this@EditPlannedViewModel.oneTime = oneTime

        saveIfEditMode()
    }

    private fun updateAmount(newAmount: Double) {
        this@EditPlannedViewModel.amount = newAmount

        saveIfEditMode()
    }

    private fun updateTitle(newTitle: String?) {
        this.title = newTitle

        saveIfEditMode()
    }

    private fun updateDescription(newDescription: String?) {
        this@EditPlannedViewModel.description = newDescription

        saveIfEditMode()
    }

    private fun updateCategory(newCategory: Category?) {
        this@EditPlannedViewModel.category = newCategory

        saveIfEditMode()
    }

    private fun updateAccount(newAccount: Account) {
        this@EditPlannedViewModel.account = newAccount
        updateCurrency(account = newAccount)

        saveIfEditMode()
    }

    private fun updateTransactionType(newTransactionType: TransactionType) {
        this@EditPlannedViewModel.transactionType = newTransactionType

        saveIfEditMode()
    }

    private fun saveIfEditMode() {
        if (editMode) {
            save(false)
        }
    }

    private fun save(closeScreen: Boolean = true) {
        val rule = rule() ?: return

        viewModelScope.launch {
            // Edit mode saves on every keystroke. Generating deletes the unpaid payments and adds
            // them again, so overlapping runs would leave duplicates: one run at a time, in order.
            saveMutex.withLock {
                plannedPaymentRuleRepository.save(rule)
                plannedPaymentsGenerator.generate(rule)
            }

            if (closeScreen) {
                nav.back()
            }
        }
    }

    /** The rule the form describes, or null while it's incomplete. */
    private fun rule(): PlannedPaymentRule? = either {
        ensure(transactionType != TransactionType.TRANSFER) { "Planned transfers aren't supported" }
        val startDate = ensureNotNull(startDate) { "No start date" }
        PlannedPaymentRule(
            id = ensureNotNull(ruleId) { "No rule" },
            type = transactionType,
            account = ensureNotNull(account) { "No account" }.id,
            amount = PositiveDouble.from(amount).bind(),
            category = category?.id,
            title = title?.let(NotBlankTrimmedString::from)?.getOrNull(),
            description = description?.let(NotBlankTrimmedString::from)?.getOrNull(),
            startDate = with(timeConverter) { startDate.toUTC() },
            interval = if (oneTime) {
                null
            } else {
                PlannedPaymentInterval(
                    n = PositiveInt.from(ensureNotNull(intervalN) { "No interval" }).bind(),
                    type = ensureNotNull(intervalType) { "No interval type" },
                )
            },
        )
    }.getOrNull()

    private fun delete() {
        viewModelScope.launch {
            deleteTransactionModalVisible = false
            ruleId?.let {
                plannedPaymentRuleRepository.deleteById(it)
                transactionRepository.deletedByRecurringRuleIdAndNoDateTime(recurringRuleId = it.value)
            }
            nav.back()
        }
    }

    private fun createCategory(data: CreateCategoryData) {
        viewModelScope.launch {
            categoryCreator.createCategory(data) {
                categories = categoryRepository.findAll().toImmutableList()

                updateCategory(it)
            }
        }
    }

    private fun editCategory(updatedCategory: Category) {
        viewModelScope.launch {
            categoryCreator.editCategory(updatedCategory) {
                categories = categoryRepository.findAll().toImmutableList()
            }
        }
    }

    private fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            accountCreator.createAccount(data) {
                accounts = accountsAct()
            }
        }
    }

    private fun reset() {
        ruleId = null

        initialTitle = null
        description = null
        category = null
    }
}
