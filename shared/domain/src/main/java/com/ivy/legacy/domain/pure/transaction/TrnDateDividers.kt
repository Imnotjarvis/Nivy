package com.ivy.legacy.domain.pure.transaction

import arrow.core.Option
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Account
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.Transaction
import com.ivy.frp.Pure
import com.ivy.frp.SideEffect
import com.ivy.legacy.domain.data.TransactionHistoryDateDivider
import com.ivy.legacy.domain.data.TransactionHistoryEntry
import com.ivy.legacy.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.domain.pure.exchange.ExchangeTrnArgument
import com.ivy.wallet.domain.pure.exchange.exchangeInBaseCurrency
import com.ivy.wallet.domain.pure.transaction.expenses
import com.ivy.wallet.domain.pure.transaction.incomes
import com.ivy.wallet.domain.pure.transaction.sumTrns
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.UUID

/** Groups [transactions] by local day, newest first, each day headed by its income and expenses. */
@Pure
suspend fun transactionsWithDateDividers(
    transactions: List<Transaction>,
    baseCurrencyCode: String,
    timeConverter: TimeConverter,
    @SideEffect
    getAccount: suspend (accountId: UUID) -> Account?,
    @SideEffect
    exchange: suspend (ExchangeData, BigDecimal) -> Option<BigDecimal>,
    @SideEffect
    getTags: suspend (tagIds: List<TagId>) -> List<Tag> = { emptyList() },
): List<TransactionHistoryItem> {
    if (transactions.isEmpty()) return emptyList()
    val arg = ExchangeTrnArgument(
        baseCurrency = baseCurrencyCode,
        getAccount = getAccount,
        exchange = exchange
    )
    return transactions
        .groupBy { with(timeConverter) { it.time.toLocalDate() } }
        .toSortedMap(compareByDescending { it })
        .flatMap { (date, transactionsForDate) ->
            val divider = TransactionHistoryDateDivider(
                date = date,
                income = sumTrns(
                    incomes(transactionsForDate),
                    ::exchangeInBaseCurrency,
                    arg
                ).toDouble(),
                expenses = sumTrns(
                    expenses(transactionsForDate),
                    ::exchangeInBaseCurrency,
                    arg
                ).toDouble()
            )
            listOf<TransactionHistoryItem>(divider) + transactionsForDate.map {
                TransactionHistoryEntry(
                    transaction = it,
                    tags = getTags(it.tags).toImmutableList(),
                )
            }
        }
}
