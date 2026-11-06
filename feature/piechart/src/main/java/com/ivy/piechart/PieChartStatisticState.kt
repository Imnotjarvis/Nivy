package com.ivy.piechart

import androidx.compose.runtime.Immutable
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Transaction
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.ivy.navigation.BudgetPage
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.domain.data.TransactionHistoryItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Immutable
data class PieChartStatisticState(
    val transactionType: TransactionType,
    val period: TimePeriod,
    val baseCurrency: String,
    val totalAmount: Double,
    val categoryAmounts: ImmutableList<CategoryAmount>,
    val selectedCategory: SelectedCategory?,
    val accountIdFilterList: ImmutableList<UUID>,
    val showCloseButtonOnly: Boolean,
    val filterExcluded: Boolean,
    val transactions: ImmutableList<Transaction>,
    val choosePeriodModal: ChoosePeriodModalData?,
    val budgetPage: BudgetPage? = null,
    val history: ImmutableList<TransactionHistoryItem> = persistentListOf(),
    val baseData: AppBaseData = AppBaseData("", persistentListOf(), persistentListOf()),
    val compactTransactions: Boolean = false,
    val showAccountColors: Boolean = false,
)
