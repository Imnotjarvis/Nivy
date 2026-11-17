package com.nivy.quickadd.edit

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
import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.model.pickable
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.QuickAddRepository
import com.nivy.domain.UserWrites
import com.nivy.navigation.EditQuickAddScreen
import com.nivy.navigation.Navigation
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.wallet.domain.logic.AccountCreator
import com.nivy.wallet.domain.logic.CategoryCreator
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import com.nivy.wallet.ui.theme.modal.edit.CategoryModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * A quick add's editor, as a planned payment's without its schedule: its type, title, category,
 * account, or two for a transfer, the card it's paid with, and an amount, or none to enter each
 * time. A quick add already saved is saved on every change; a new one when Set is pressed.
 */
@Stable
@HiltViewModel
class EditQuickAddViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val creditCardRepository: CreditCardRepository,
    private val quickAddRepository: QuickAddRepository,
    private val categoryCreator: CategoryCreator,
    private val accountCreator: AccountCreator,
    private val nav: Navigation,
    private val userWrites: UserWrites,
) : ComposeViewModel<EditQuickAddState, EditQuickAddEvent>() {

    private var type by mutableStateOf(TransactionType.EXPENSE)
    private var initialTitle by mutableStateOf<String?>(null)
    private var title: String? = null
    private var account by mutableStateOf<Account?>(null)
    private var toAccount by mutableStateOf<Account?>(null)
    private var allCreditCards by mutableStateOf<List<CreditCard>>(emptyList())
    private var creditCard by mutableStateOf<CreditCard?>(null)
    private var category by mutableStateOf<Category?>(null)
    private var amount by mutableDoubleStateOf(0.0)
    private var currency by mutableStateOf("")
    private var categories by mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var categoryModalVisible by mutableStateOf(false)
    private var categoryModalData by mutableStateOf<CategoryModalData?>(null)
    private var accountModalData by mutableStateOf<AccountModalData?>(null)
    private var deleteModalVisible by mutableStateOf(false)
    private var typeModalVisible by mutableStateOf(false)
    private var amountModalVisible by mutableStateOf(false)

    private var id: QuickAddId? = null
    private var orderNum = 0.0
    private var editMode = false

    @Composable
    override fun uiState(): EditQuickAddState = EditQuickAddState(
        type = type,
        initialTitle = initialTitle,
        currency = currency,
        categories = categories,
        accounts = accounts,
        account = account,
        toAccount = toAccount,
        creditCards = accountCreditCards(),
        creditCard = creditCard,
        category = category,
        amount = amount,
        categoryModalVisible = categoryModalVisible,
        categoryModalData = categoryModalData,
        accountModalData = accountModalData,
        deleteModalVisible = deleteModalVisible,
        typeModalVisible = typeModalVisible,
        amountModalVisible = amountModalVisible,
        canSave = quickAdd() != null,
    )

    override fun onEvent(event: EditQuickAddEvent) {
        when (event) {
            is EditQuickAddEvent.OnTypeChanged -> updateType(event.type)
            is EditQuickAddEvent.OnTitleChanged -> changed { title = event.title }
            is EditQuickAddEvent.OnAmountChanged -> changed { amount = event.amount }
            is EditQuickAddEvent.OnAccountChanged -> updateAccount(event.account)
            is EditQuickAddEvent.OnToAccountChanged -> changed { toAccount = event.account }
            is EditQuickAddEvent.OnCreditCardChanged -> changed { creditCard = event.creditCard }
            is EditQuickAddEvent.OnCategoryChanged -> changed { category = event.category }
            is EditQuickAddEvent.OnCreateCategory -> createCategory(event.data)
            is EditQuickAddEvent.OnEditCategory -> editCategory(event.category)
            is EditQuickAddEvent.OnCreateAccount -> createAccount(event.data)
            is EditQuickAddEvent.OnCategoryModalVisible -> categoryModalVisible = event.visible
            is EditQuickAddEvent.OnCategoryModalDataChanged -> categoryModalData = event.data
            is EditQuickAddEvent.OnAccountModalDataChanged -> accountModalData = event.data
            is EditQuickAddEvent.OnTypeModalVisible -> typeModalVisible = event.visible
            is EditQuickAddEvent.OnAmountModalVisible -> amountModalVisible = event.visible
            is EditQuickAddEvent.OnDeleteModalVisible -> deleteModalVisible = event.visible
            EditQuickAddEvent.OnSave -> save(closeScreen = true)
            EditQuickAddEvent.OnDelete -> delete()
        }
    }

    fun start(screen: EditQuickAddScreen) {
        viewModelScope.launch {
            val all = accountRepository.findAll()
            if (all.isEmpty()) {
                nav.back()
                return@launch
            }
            categories = categoryRepository.findAll().toImmutableList()
            allCreditCards = creditCardRepository.findAll()

            val quickAdd = screen.quickAddId?.let { quickAddRepository.findById(QuickAddId(it)) }
            editMode = quickAdd != null
            // An archived account a quick add already uses, or goes to, stays there to pick.
            val pickable = all.pickable(listOf(quickAdd?.account, quickAdd?.toAccount)).ifEmpty { all }
            accounts = pickable.toImmutableList()
            if (quickAdd != null) display(quickAdd) else displayNew(screen, pickable)
        }
    }

    private suspend fun display(quickAdd: QuickAdd) {
        id = quickAdd.id
        orderNum = quickAdd.orderNum
        type = quickAdd.type
        title = quickAdd.title?.value
        initialTitle = title
        account = accountRepository.findById(quickAdd.account)
        toAccount = quickAdd.toAccount?.let { accountRepository.findById(it) }
        creditCard = allCreditCards.find { it.id == quickAdd.creditCard }
        category = quickAdd.category?.let { categoryRepository.findById(it) }
        amount = quickAdd.amount?.value ?: 0.0
        account?.let { currency = it.asset.code }
    }

    private suspend fun displayNew(screen: EditQuickAddScreen, accounts: List<Account>) {
        id = QuickAddId(UUID.randomUUID())
        // After the others.
        orderNum = (quickAddRepository.findAll().maxOfOrNull { it.orderNum } ?: -1.0) + 1.0
        type = screen.type
        title = screen.title
        initialTitle = screen.title
        val account = accounts.find { it.id.value == screen.accountId } ?: accounts.first()
        this.account = account
        toAccount = screen.toAccountId?.let { id -> accounts.find { it.id.value == id } }
        creditCard = screen.creditCardId?.let { id -> allCreditCards.find { it.id.value == id } }
            ?: defaultCreditCard(account, type)
        category = screen.categoryId?.let { categoryRepository.findById(CategoryId(it)) }
        amount = screen.amount ?: 0.0
        currency = account.asset.code
    }

    private fun updateType(newType: TransactionType) {
        // Only an expense or a transfer is paid with a card.
        if (newType != type) creditCard = account?.let { defaultCreditCard(it, newType) }
        changed { type = newType }
    }

    private fun updateAccount(newAccount: Account) {
        // Another account's card can't pay it: the one its new expenses start on instead.
        if (newAccount.id != account?.id) creditCard = defaultCreditCard(newAccount, type)
        currency = newAccount.asset.code
        changed { account = newAccount }
    }

    /** The cards of the account an expense or a transfer can be paid with. */
    private fun accountCreditCards(): ImmutableList<CreditCard> =
        if (type != TransactionType.INCOME) {
            allCreditCards.filter { it.account == account?.id }.toImmutableList()
        } else {
            persistentListOf()
        }

    /** What a new expense on [account] is paid with: a credit card account's own card. */
    private fun defaultCreditCard(account: Account, type: TransactionType): CreditCard? =
        allCreditCards.firstOrNull { it.account == account.id }
            ?.takeIf { type == TransactionType.EXPENSE && account.type == AccountType.CREDIT_CARD }

    // A quick add already saved keeps every change at once.
    private fun changed(change: () -> Unit) {
        change()
        if (editMode) save(closeScreen = false)
    }

    private fun save(closeScreen: Boolean) {
        val quickAdd = quickAdd() ?: return
        viewModelScope.launch {
            val saved = userWrites.run(R.string.msg_change_not_saved) { quickAddRepository.save(quickAdd) }
            if (saved && closeScreen) nav.back()
        }
    }

    /** The quick add the form describes, or null while it's incomplete. */
    private fun quickAdd(): QuickAdd? = either {
        val account = ensureNotNull(account) { "No account" }.id
        val toAccount = if (type == TransactionType.TRANSFER) {
            ensureNotNull(toAccount) { "No account to transfer to" }.id
                .also { ensure(it != account) { "A transfer to the account it leaves" } }
        } else {
            null
        }
        QuickAdd(
            id = ensureNotNull(id) { "No quick add" },
            type = type,
            account = account,
            toAccount = toAccount,
            // None: it's entered each time.
            amount = PositiveDouble.from(amount).getOrNull(),
            category = category?.id,
            title = title?.let(NotBlankTrimmedString::from)?.getOrNull(),
            creditCard = creditCard?.id,
            orderNum = orderNum,
        )
    }.getOrNull()

    private fun delete() {
        deleteModalVisible = false
        val id = id ?: return
        viewModelScope.launch {
            val deleted = userWrites.run(R.string.msg_change_not_saved) { quickAddRepository.deleteById(id) }
            if (deleted) nav.back()
        }
    }

    private fun createCategory(data: CreateCategoryData) {
        viewModelScope.launch {
            categoryCreator.createCategory(data) {
                categories = categoryRepository.findAll().toImmutableList()
                changed { category = it }
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
                accounts = accountRepository.findAll().pickable(listOf(account?.id)).toImmutableList()
                allCreditCards = creditCardRepository.findAll()
            }
        }
    }
}
