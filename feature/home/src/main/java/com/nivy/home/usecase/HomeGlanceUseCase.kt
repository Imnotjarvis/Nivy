package com.nivy.home.usecase

import androidx.compose.runtime.Immutable
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.getFromValue
import com.nivy.domain.usecase.wallet.WalletIncomeExpenseUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** How many days after today the glance looks for the next payment. */
private const val NextPaymentWithinDays = 7L

/**
 * What Home's header shows in place of a greeting. It's always about today, whatever month Home
 * shows: the payments overdue, what was spent today and the next payment due within a week.
 */
class HomeGlanceUseCase @Inject constructor(
    private val overdueTransactionsUseCase: OverdueTransactionsUseCase,
    private val upcomingTransactionsUseCase: UpcomingTransactionsUseCase,
    private val walletIncomeExpenseUseCase: WalletIncomeExpenseUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) {
    suspend fun glance(
        baseCurrency: String,
        accounts: List<Account>,
        categories: List<Category>,
    ): Glance = with(timeConverter) {
        val today = timeProvider.localDateNow()
        val startOfToday = today.atStartOfDay().toUTC()
        val endOfToday = today.atTime(LocalTime.MAX).toUTC()

        val overdue = overdueTransactionsUseCase.overdue(toRange = endOfToday, baseCurrency = baseCurrency)
        // Counted as the month's expenses are, over today: an amount without a rate is left out.
        val spentToday = walletIncomeExpenseUseCase.incomeExpense(
            baseCurrency = baseCurrency,
            accounts = accounts,
            range = ClosedTimeRange(from = startOfToday, to = endOfToday),
        ).expense
        val next = upcomingTransactionsUseCase.upcoming(
            range = ClosedTimeRange(
                from = startOfToday,
                to = today.plusDays(NextPaymentWithinDays).atTime(LocalTime.MAX).toUTC(),
            ),
            baseCurrency = baseCurrency,
        ).upcomingTrns.minByOrNull { it.time }

        Glance(
            overdue = overdue.overdueTrns.size,
            spentToday = spentToday,
            baseCurrency = baseCurrency,
            nextPayment = next?.let { nextPayment(it, categories, today) },
        )
    }

    private fun nextPayment(transaction: Transaction, categories: List<Category>, today: LocalDate): NextPayment {
        val value = transaction.getFromValue()
        val dueDate = with(timeConverter) { transaction.time.toLocalDate() }
        return NextPayment(
            name = transaction.title?.value ?: categories.firstOrNull { it.id == transaction.category }?.name?.value,
            type = when (transaction) {
                is Income -> TransactionType.INCOME
                is Expense -> TransactionType.EXPENSE
                is Transfer -> TransactionType.TRANSFER
            },
            amount = value.amount.value,
            currency = value.asset.code,
            daysLeft = ChronoUnit.DAYS.between(today, dueDate).toInt(),
        )
    }
}

@Immutable
data class Glance(
    /** How many payments are overdue. */
    val overdue: Int,
    /** In the base currency. */
    val spentToday: BigDecimal,
    val baseCurrency: String,
    /** The first payment due from today to a week after, if any. */
    val nextPayment: NextPayment?,
)

@Immutable
data class NextPayment(
    /** Its title, else its category's name; null when it has neither. */
    val name: String?,
    val type: TransactionType,
    /** In its own currency: one payment, nothing to add up. */
    val amount: Double,
    val currency: String,
    /** 0 when it's due today, 1 tomorrow. */
    val daysLeft: Int,
)
