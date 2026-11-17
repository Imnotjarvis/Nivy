package com.nivy.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.TransactionId
import com.nivy.data.model.getFromValue
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.QuickAddRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.quickadd.QuickAddUseCase
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.Navigation
import com.nivy.ui.R
import com.nivy.wallet.MainScreenState
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.data.model.MainTab
import com.nivy.wallet.domain.logic.AccountCreator
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.utils.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What a quick add just saved: the transaction, to take back, and what it was. */
data class QuickAdded(val transaction: TransactionId, val quickAdd: QuickAdd, val value: PositiveValue)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainScreenState: MainScreenState,
    private val accountCreator: AccountCreator,
    private val currencyRepository: CurrencyRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val quickAddRepository: QuickAddRepository,
    private val quickAddUseCase: QuickAddUseCase,
    private val userWrites: UserWrites,
    private val nav: Navigation,
) : ViewModel() {

    private val _currency = MutableLiveData<String>()
    val currency = _currency.asLiveData()

    /** The quick adds the add menu shows, in their order. */
    var quickAdds by mutableStateOf<ImmutableList<QuickAdd>>(persistentListOf())
        private set

    /** The accounts and categories the quick adds show. */
    var baseData by mutableStateOf(AppBaseData("", persistentListOf(), persistentListOf()))
        private set

    /** The quick add whose amount is being entered, as it has none of its own. */
    var amountAsked by mutableStateOf<QuickAdd?>(null)
        private set

    /** What the last quick add saved, while it can still be taken back. */
    var added by mutableStateOf<QuickAdded?>(null)
        private set

    private var undoShown: Job? = null

    /** Again on coming back to the main screen: the quick adds may have changed meanwhile. */
    fun start() {
        viewModelScope.launch {
            val baseCurrency = currencyRepository.getBaseCurrency().code
            _currency.value = baseCurrency
            baseData = AppBaseData(
                baseCurrency = baseCurrency,
                accounts = accountRepository.findAll().toImmutableList(),
                categories = categoryRepository.findAll().toImmutableList(),
            )
            quickAdds = quickAddRepository.findAll().toImmutableList()
        }
    }

    fun selectTab(tab: MainTab) {
        mainScreenState.selectTab(tab)
    }

    fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            accountCreator.createAccount(data) {}
        }
    }

    /** Adds [quickAdd] now; one without an amount asks for it first. */
    fun quickAdd(quickAdd: QuickAdd) {
        if (quickAdd.amount == null) amountAsked = quickAdd else add(quickAdd, amount = null)
    }

    /** The amount entered for the quick add that asked for it. */
    fun enterAmount(amount: Double) {
        val quickAdd = amountAsked ?: return
        amountAsked = null
        PositiveDouble.from(amount).getOrNull()?.let { add(quickAdd, it) }
    }

    fun dismissAmount() {
        amountAsked = null
    }

    /** Opens the transaction editor filled in with [quickAdd], to change it before it's saved. */
    fun edit(quickAdd: QuickAdd, amount: PositiveDouble? = null) {
        nav.navigateTo(
            EditTransactionScreen(
                initialTransactionId = null,
                type = quickAdd.type,
                accountId = quickAdd.account.value,
                categoryId = quickAdd.category?.value,
                toAccountId = quickAdd.toAccount?.value,
                title = quickAdd.title?.value,
                amount = (quickAdd.amount ?: amount)?.value,
                creditCardId = quickAdd.creditCard?.value,
            )
        )
    }

    /** Takes back what the last quick add saved. */
    fun undo() {
        val last = added ?: return
        hideUndo()
        viewModelScope.launch {
            userWrites.run(R.string.msg_change_not_saved) { quickAddUseCase.undo(last.transaction) }
        }
    }

    private fun add(quickAdd: QuickAdd, amount: PositiveDouble?) {
        viewModelScope.launch {
            var saved: QuickAdded? = null
            var unsaved = false
            val wrote = userWrites.run(R.string.msg_transaction_not_saved) {
                val transaction = quickAddUseCase.add(quickAdd, amount)
                if (transaction == null) {
                    unsaved = true
                } else {
                    saved = QuickAdded(transaction.id, quickAdd, transaction.getFromValue())
                }
            }
            when {
                !wrote -> Unit
                // A transfer between currencies without a rate: the editor asks for one.
                unsaved -> edit(quickAdd, amount)
                else -> saved?.let(::showUndo)
            }
        }
    }

    private fun showUndo(quickAdded: QuickAdded) {
        added = quickAdded
        undoShown?.cancel()
        undoShown = viewModelScope.launch {
            delay(UndoShownMillis)
            added = null
        }
    }

    private fun hideUndo() {
        undoShown?.cancel()
        added = null
    }

    private companion object {
        // Long enough to read it and reach Undo.
        const val UndoShownMillis = 5_000L
    }
}
