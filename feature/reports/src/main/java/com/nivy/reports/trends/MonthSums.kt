package com.nivy.reports.trends

import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.primitive.AssetCode
import com.nivy.wallet.domain.pure.data.ClosedTimeRange

/** What a month's transactions add up to, per currency. */
internal data class MonthSums(
    val income: Map<AssetCode, Double>,
    val expense: Map<AssetCode, Double>,
)

/**
 * Sums the paid [transactions] of the [accounts] for each of the [months]. Transfers aren't income
 * or expenses.
 */
internal fun monthSums(
    transactions: List<Transaction>,
    accounts: Set<AccountId>,
    months: List<ClosedTimeRange>,
): List<MonthSums> {
    val paid = transactions.filter { it.settled }
    return months.map { range ->
        val inMonth = paid.filter { !it.time.isBefore(range.from) && !it.time.isAfter(range.to) }
        MonthSums(
            income = inMonth.filterIsInstance<Income>()
                .filter { it.account in accounts }
                .map { it.value.asset to it.value.amount.value }
                .sumByAsset(),
            expense = inMonth.filterIsInstance<Expense>()
                .filter { it.account in accounts }
                .map { it.value.asset to it.value.amount.value }
                .sumByAsset(),
        )
    }
}

private fun List<Pair<AssetCode, Double>>.sumByAsset(): Map<AssetCode, Double> =
    groupBy({ it.first }, { it.second }).mapValues { (_, amounts) -> amounts.sum() }
