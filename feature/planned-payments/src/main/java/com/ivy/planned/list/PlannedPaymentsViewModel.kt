package com.ivy.planned.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Account
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.PlannedPaymentRuleRepository
import com.ivy.domain.usecase.planned.PlannedPaymentAmountsUseCase
import com.ivy.ui.ComposeViewModel
import com.ivy.data.model.Category
import com.ivy.data.repository.CategoryRepository
import com.ivy.wallet.domain.action.account.AccountsAct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class PlannedPaymentsViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
    private val plannedPaymentAmountsUseCase: PlannedPaymentAmountsUseCase,
    private val categoriesRepository: CategoryRepository,
    private val accountsAct: AccountsAct
) : ComposeViewModel<PlannedPaymentsScreenState, PlannedPaymentsScreenEvent>() {

    private var currency by mutableStateOf("")
    private var categories by mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var oneTimePlannedPayment by
        mutableStateOf<ImmutableList<PlannedPaymentRule>>(persistentListOf())
    private var recurringPlannedPayment by
        mutableStateOf<ImmutableList<PlannedPaymentRule>>(persistentListOf())
    private var oneTimeIncome by mutableDoubleStateOf(0.0)
    private var oneTimeExpenses by mutableDoubleStateOf(0.0)
    private var recurringIncome by mutableDoubleStateOf(0.0)
    private var recurringExpenses by mutableDoubleStateOf(0.0)
    private var isOneTimePaymentsExpanded by mutableStateOf(true)
    private var isRecurringPaymentsExpanded by mutableStateOf(true)

    @Composable
    override fun uiState(): PlannedPaymentsScreenState {
        LaunchedEffect(Unit) {
            start()
        }

        return PlannedPaymentsScreenState(
            currency = getCurrency(),
            categories = getCategories(),
            accounts = getAccounts(),
            oneTimeIncome = getOneTimeIncome(),
            oneTimeExpenses = getOneTimeExpenses(),
            recurringExpenses = getRecurringExpenses(),
            recurringIncome = getRecurringIncome(),
            recurringPlannedPayment = getRecurringPlannedPayment(),
            oneTimePlannedPayment = getOneTimePlannedPayment(),
            isOneTimePaymentsExpanded = getOneTimePaymentsExpanded(),
            isRecurringPaymentsExpanded = getRecurringPaymentsExpanded()
        )
    }

    @Composable
    private fun getCurrency(): String {
        return currency
    }

    @Composable
    private fun getCategories(): ImmutableList<Category> {
        return categories
    }

    @Composable
    private fun getAccounts(): ImmutableList<Account> {
        return accounts
    }

    @Composable
    private fun getOneTimePlannedPayment(): ImmutableList<PlannedPaymentRule> {
        return oneTimePlannedPayment
    }

    @Composable
    private fun getRecurringPlannedPayment(): ImmutableList<PlannedPaymentRule> {
        return recurringPlannedPayment
    }

    @Composable
    private fun getOneTimeExpenses(): Double {
        return oneTimeExpenses
    }

    @Composable
    private fun getOneTimeIncome(): Double {
        return oneTimeIncome
    }

    @Composable
    private fun getRecurringExpenses(): Double {
        return recurringExpenses
    }

    @Composable
    private fun getRecurringIncome(): Double {
        return recurringIncome
    }

    @Composable
    private fun getRecurringPaymentsExpanded(): Boolean {
        return isRecurringPaymentsExpanded
    }

    @Composable
    private fun getOneTimePaymentsExpanded(): Boolean {
        return isOneTimePaymentsExpanded
    }

    override fun onEvent(event: PlannedPaymentsScreenEvent) {
        when (event) {
            is PlannedPaymentsScreenEvent.OnOneTimePaymentsExpanded -> {
                isOneTimePaymentsExpanded = event.isExpanded
            }
            is PlannedPaymentsScreenEvent.OnRecurringPaymentsExpanded -> {
                isRecurringPaymentsExpanded = event.isExpanded
            }
        }
    }

    private fun start() {
        viewModelScope.launch {
            val baseCurrency = currencyRepository.getBaseCurrency()
            currency = baseCurrency.code

            categories = categoriesRepository.findAll().toImmutableList()
            accounts = accountsAct()

            val oneTime = plannedPaymentRuleRepository.findAllByOneTime(oneTime = true)
            oneTimePlannedPayment = oneTime.toImmutableList()
            oneTimeIncome = plannedPaymentAmountsUseCase.total(
                rules = oneTime.filter { it.type == TransactionType.INCOME },
                currency = baseCurrency,
            )
            oneTimeExpenses = plannedPaymentAmountsUseCase.total(
                rules = oneTime.filter { it.type == TransactionType.EXPENSE },
                currency = baseCurrency,
            )

            val recurring = plannedPaymentRuleRepository.findAllByOneTime(oneTime = false)
            recurringPlannedPayment = recurring.toImmutableList()
            recurringIncome = plannedPaymentAmountsUseCase.monthlyTotal(
                rules = recurring.filter { it.type == TransactionType.INCOME },
                currency = baseCurrency,
            )
            recurringExpenses = plannedPaymentAmountsUseCase.monthlyTotal(
                rules = recurring.filter { it.type == TransactionType.EXPENSE },
                currency = baseCurrency,
            )
        }
    }
}
