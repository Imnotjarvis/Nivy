package com.ivy.transactions

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Account
import com.ivy.data.model.AccountValue
import com.ivy.data.model.Category
import com.ivy.data.model.CreditCard
import com.ivy.data.model.Transaction
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class TransactionsState(
    val period: TimePeriod,
    val baseCurrency: String,
    val currency: String,
    val categories: ImmutableList<Category>,
    val accounts: ImmutableList<Account>,
    val account: Account?,
    // The account's credit cards, which its dialog edits.
    val creditCards: ImmutableList<CreditCard>,
    val category: Category?,
    val balance: Double,
    val balanceBaseCurrency: Double?,
    val income: Double,
    val expenses: Double,
    val initWithTransactions: Boolean,
    val treatTransfersAsIncomeExpense: Boolean,
    val history: ImmutableList<TransactionHistoryItem>,
    val upcoming: ImmutableList<Transaction>,
    val upcomingExpanded: Boolean,
    val upcomingIncome: Double,
    val upcomingExpenses: Double,
    val overdue: ImmutableList<Transaction>,
    val overdueExpanded: Boolean,
    val overdueIncome: Double,
    val overdueExpenses: Double,
    val enableDeletionButton: Boolean,
    val skipAllModalVisible: Boolean,
    val deleteModal1Visible: Boolean,
    val choosePeriodModal: ChoosePeriodModalData?,
    val showAccountColorsInTransactions: Boolean,
    val compactTransactions: Boolean = false,
    // An investment account's money in, gain and values; null for other accounts and categories.
    val investment: InvestmentData? = null,
)

/**
 * @property invested the money put in and invested: what the account's transactions add up to,
 * less its [cash].
 * @property gain how much more (or less) it's worth; null before a value was entered.
 * @property cash the money put in that isn't invested, when the last value has its cash.
 * @property values the values entered, the latest first.
 */
@Immutable
data class InvestmentData(
    val invested: Double,
    val gain: Double?,
    val cash: Double?,
    val transfersIn: Int,
    val values: ImmutableList<ValueEntry>,
    val valuesExpanded: Boolean,
)

/** A [value] entered, with the money invested by then: put in, less the value's cash. */
@Immutable
data class ValueEntry(
    val value: AccountValue,
    val invested: Double,
)
