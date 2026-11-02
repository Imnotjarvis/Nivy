package com.ivy.reports.trends

import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import java.time.Instant

/** What a month's transactions add up to, per currency. */
internal data class MonthSums(
    val income: Map<AssetCode, Double>,
    val expense: Map<AssetCode, Double>,
    // What the accounts hold at the month's end, or now for the current month.
    val netWorth: Map<AssetCode, Double>,
)

/**
 * Sums the paid [transactions] of the [accounts] for each of the [months]. Transfers aren't income
 * or expenses; they count only in the net worth, as money moving between accounts.
 */
internal fun monthSums(
    transactions: List<Transaction>,
    accounts: Set<AccountId>,
    months: List<ClosedTimeRange>,
    now: Instant,
): List<MonthSums> {
    val paid = transactions.filter { it.settled }
    return months.map { range ->
        val inMonth = paid.filter { !it.time.isBefore(range.from) && !it.time.isAfter(range.to) }
        val end = minOf(range.to, now)
        MonthSums(
            income = inMonth.filterIsInstance<Income>()
                .filter { it.account in accounts }
                .map { it.value.asset to it.value.amount.value }
                .sumByAsset(),
            expense = inMonth.filterIsInstance<Expense>()
                .filter { it.account in accounts }
                .map { it.value.asset to it.value.amount.value }
                .sumByAsset(),
            netWorth = paid.filter { !it.time.isAfter(end) }
                .flatMap { it.balanceChanges(accounts) }
                .sumByAsset(),
        )
    }
}

/** How the transaction changes the balances of the [accounts]. */
private fun Transaction.balanceChanges(accounts: Set<AccountId>): List<Pair<AssetCode, Double>> =
    when (this) {
        is Income -> listOfNotNull((value.asset to value.amount.value).takeIf { account in accounts })
        is Expense -> listOfNotNull((value.asset to -value.amount.value).takeIf { account in accounts })
        is Transfer -> listOfNotNull(
            (fromValue.asset to -fromValue.amount.value).takeIf { fromAccount in accounts },
            (toValue.asset to toValue.amount.value).takeIf { toAccount in accounts },
        )
    }

private fun List<Pair<AssetCode, Double>>.sumByAsset(): Map<AssetCode, Double> =
    groupBy({ it.first }, { it.second }).mapValues { (_, amounts) -> amounts.sum() }
