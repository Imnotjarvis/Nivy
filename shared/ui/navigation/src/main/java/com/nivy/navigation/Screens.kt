package com.nivy.navigation

import com.nivy.base.model.TransactionType
import com.nivy.data.model.Budget
import com.nivy.data.model.Transaction
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

data object MainScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object OnboardingScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data class CSVScreen(
    val launchedFromOnboarding: Boolean
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

data class EditTransactionScreen(
    val initialTransactionId: UUID?,
    val type: TransactionType,
    // extras
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    // A new transfer's destination: the money comes from another account.
    val toAccountId: UUID? = null,
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

data class TransactionsScreen(
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    val unspecifiedCategory: Boolean? = false,
    val transactionType: TransactionType? = null,
    val accountIdFilterList: List<UUID> = persistentListOf(),
    val transactions: List<Transaction> = persistentListOf()
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

data class PieChartStatisticScreen(
    val type: TransactionType,
    val filterExcluded: Boolean = true,
    val accountList: ImmutableList<UUID> = persistentListOf(),
    val transactions: ImmutableList<Transaction> = persistentListOf(),
    val treatTransfersAsIncomeExpense: Boolean = false,
    val budgetPage: BudgetPage? = null,
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

/**
 * The pie chart as [budget]'s page for [month], which [range] covers: headed by the budget's name
 * and the month, charting its expenses, even none, and listing them under the chart. They're read
 * each time the page shows, so one edited or deleted from it shows as it is now.
 */
data class BudgetPage(val budget: Budget, val month: String, val range: ClosedTimeRange)

data class EditPlannedScreen(
    val plannedPaymentRuleId: UUID?,
    val type: TransactionType,
    val amount: Double? = null,
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    val title: String? = null,
    val description: String? = null,
) : Screen {
    override val isLegacy: Boolean
        get() = true

    fun mandatoryFilled(): Boolean {
        return amount != null && amount > 0.0 &&
                accountId != null
    }
}

data object BalanceScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object PlannedPaymentsScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object CategoriesScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object SettingsScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data class ImportScreen(
    val launchedFromOnboarding: Boolean
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object ReportScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

/**
 * Budgets. Each opening is its own screen: gone back to, it's the same one, and shows the month it
 * showed; opened anew, it shows the current month.
 */
class BudgetScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object LoansScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object SearchScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data class LoanDetailsScreen(
    val loanId: UUID
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object ExchangeRatesScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

data object FeaturesScreen : Screen

data object TrendsScreen : Screen
