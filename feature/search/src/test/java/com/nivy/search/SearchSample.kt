package com.nivy.search

import androidx.compose.ui.graphics.Color
import com.nivy.base.model.TransactionType
import com.nivy.base.time.impl.FixedTimeProvider
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.design.system.colors.Palette
import com.nivy.wallet.data.DueSection
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewCategory
import com.nivy.wallet.ui.previewTransaction
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/** What Search shows for a salary and its plan, and for a restaurant, on November 18, 2026. */
internal object SearchSample {
    /** November 18, 2026, at noon, in UTC. */
    val November18 = FixedTimeProvider(LocalDateTime.of(2026, 11, 18, 12, 0))

    private val bank = previewAccount("Main Bank", Palette.Blue3, icon = "bank", currency = "EUR", id = UUID(0, 1))
    private val work = category("Salary", Palette.Green2, "work", id = 2)
    private val eatingOut = category("Eating out", Palette.Red3, "restaurant", id = 3)

    // Paid on the 27th, January to October.
    private val paidSalaries = (1..10).map { month ->
        previewTransaction(
            type = TransactionType.INCOME,
            account = bank,
            amount = 1_800.0,
            time = at(LocalDate.of(2026, month, 27)),
            title = "Salary",
            category = work.id,
            id = UUID(1, month.toLong()),
        )
    }

    // The plan's payments to come: three years of them, from November 27.
    private val plannedSalaries = (0 until 36).map { months ->
        previewTransaction(
            type = TransactionType.INCOME,
            account = bank,
            amount = 1_800.0,
            time = at(LocalDate.of(2026, 11, 27).plusMonths(months.toLong())),
            settled = false,
            title = "Salary",
            category = work.id,
            id = UUID(2, months.toLong()),
        )
    }

    private val dinners = listOf(
        LocalDate.of(2026, 11, 14) to 24.5,
        LocalDate.of(2026, 11, 1) to 31.8,
        LocalDate.of(2026, 10, 24) to 18.0,
        LocalDate.of(2026, 10, 10) to 42.2,
        LocalDate.of(2026, 9, 26) to 27.9,
        LocalDate.of(2026, 9, 12) to 22.4,
        LocalDate.of(2026, 8, 29) to 19.6,
    ).mapIndexed { i, (date, amount) ->
        previewTransaction(
            type = TransactionType.EXPENSE,
            account = bank,
            amount = amount,
            time = at(date),
            title = "Pizzeria da Gino",
            category = eatingOut.id,
            id = UUID(3, i.toLong()),
        )
    }

    val Salary = state(query = "Salary", paid = paidSalaries, upcoming = plannedSalaries)
    val Restaurant = state(query = "Pizzeria", paid = dinners, upcoming = emptyList())

    private fun state(query: String, paid: List<Transaction>, upcoming: List<Transaction>) = SearchState(
        searchQuery = query,
        transactions = history(paid).toImmutableList(),
        upcoming = DueSection(trns = upcoming.toImmutableList(), expanded = false, stats = totals(upcoming)),
        overdue = DueSection(trns = persistentListOf(), expanded = false, stats = IncomeExpensePair.zero()),
        income = totals(paid).income.toDouble(),
        expenses = totals(paid).expense.toDouble(),
        baseCurrency = "EUR",
        accounts = persistentListOf(bank),
        categories = persistentListOf(work, eatingOut),
        shouldShowAccountSpecificColorInTransactions = false,
    )

    // Newest first, by day, each day headed by its income and expenses.
    private fun history(transactions: List<Transaction>): List<TransactionHistoryItem> =
        transactions.sortedByDescending { it.time }
            .groupBy { LocalDate.ofInstant(it.time, ZoneOffset.UTC) }
            .flatMap { (date, day) ->
                val totals = totals(day)
                listOf(TransactionHistoryDateDivider(date, totals.income.toDouble(), totals.expense.toDouble())) +
                    day.map { TransactionHistoryEntry(it) }
            }

    private fun totals(transactions: List<Transaction>) = IncomeExpensePair(
        income = transactions.filterIsInstance<Income>().sumOf { it.value.amount.value }.toBigDecimal(),
        expense = transactions.filterIsInstance<Expense>().sumOf { it.value.amount.value }.toBigDecimal(),
    )

    private fun at(date: LocalDate): Instant = date.atTime(9, 0).toInstant(ZoneOffset.UTC)

    private fun category(name: String, color: Color, icon: String, id: Long) =
        previewCategory(name, color, icon, id = UUID(0, id))
}
