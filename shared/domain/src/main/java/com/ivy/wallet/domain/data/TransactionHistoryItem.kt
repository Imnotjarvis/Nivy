package com.ivy.wallet.domain.data

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Tag
import com.ivy.data.model.Transaction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate

/** A row of a transaction history: a day's header or one of its transactions. */
@Immutable
sealed interface TransactionHistoryItem

@Immutable
data class TransactionHistoryDateDivider(
    val date: LocalDate,
    val income: Double,
    val expenses: Double
) : TransactionHistoryItem

@Immutable
data class TransactionHistoryEntry(
    val transaction: Transaction,
    val tags: ImmutableList<Tag> = persistentListOf(),
) : TransactionHistoryItem
