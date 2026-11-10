package com.nivy.wallet.domain.pure.transaction

import com.nivy.base.time.TimeConverter
import com.nivy.data.model.Tag
import com.nivy.data.model.Transaction
import com.nivy.data.model.getFromValue
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.domain.data.TransactionHistoryItem
import kotlinx.collections.immutable.toImmutableList

/**
 * Groups [transactions] by local day, newest first, each day headed by its income and expenses;
 * each transaction comes with its tags, [tagsOf] it.
 */
suspend fun transactionsWithDateDividers(
    transactions: List<Transaction>,
    timeConverter: TimeConverter,
    rates: BaseCurrencyRates,
    tagsOf: (Transaction) -> List<Tag> = { emptyList() },
): List<TransactionHistoryItem> {
    if (transactions.isEmpty()) return emptyList()
    return transactions
        .groupBy { with(timeConverter) { it.time.toLocalDate() } }
        .toSortedMap(compareByDescending { it })
        .flatMap { (date, transactionsForDate) ->
            val divider = TransactionHistoryDateDivider(
                date = date,
                income = incomes(transactionsForDate).sumOf { rates.inBase(it.getFromValue()) },
                expenses = expenses(transactionsForDate).sumOf { rates.inBase(it.getFromValue()) },
            )
            listOf<TransactionHistoryItem>(divider) + transactionsForDate.map {
                TransactionHistoryEntry(
                    transaction = it,
                    tags = tagsOf(it).toImmutableList(),
                )
            }
        }
}
