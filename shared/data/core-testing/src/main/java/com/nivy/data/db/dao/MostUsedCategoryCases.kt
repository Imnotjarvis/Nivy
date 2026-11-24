package com.nivy.data.db.dao

import com.nivy.base.model.TransactionType
import com.nivy.data.db.entity.TransactionEntity
import org.jetbrains.annotations.VisibleForTesting
import java.time.Instant
import java.util.UUID

/**
 * The cases the categories used most must pass: the database's query (TransactionDaoMostUsedTest,
 * on a device) and FakeTransactionDao's copy of it (FakeTransactionDaoMostUsedTest), so that the
 * two can't drift apart.
 */
@VisibleForTesting
object MostUsedCategoryCases {
    val Groceries = category("Groceries")
    val Coffee = category("Coffee")
    val Restaurant = category("Restaurant")
    val Transport = category("Transport")
    private val Pharmacy = category("Pharmacy")
    private val Rent = category("Rent")
    private val Salary = category("Salary")

    /** From when uses count. */
    val Since: Instant = Instant.parse("2025-11-24T00:00:00Z")

    const val Limit = 4

    val transactions = listOf(
        expense(Groceries, "2026-11-01T10:00:00Z"),
        expense(Groceries, "2026-10-01T10:00:00Z"),
        expense(Groceries, "2026-09-01T10:00:00Z"),
        // Used as often: the one used last comes first.
        expense(Restaurant, "2026-11-10T20:00:00Z"),
        expense(Restaurant, "2026-11-11T20:00:00Z"),
        expense(Coffee, "2026-01-10T08:00:00Z"),
        expense(Coffee, "2026-11-20T08:00:00Z"),
        expense(Pharmacy, "2026-02-01T10:00:00Z"),
        expense(Transport, "2026-05-01T10:00:00Z"),
    ) + List(4) {
        // Before the time asked from: not counted.
        expense(Rent, "2025-06-01T10:00:00Z")
    } + List(5) {
        // Another type, still to pay, or without a category: not counted.
        listOf(
            expense(Salary, "2026-10-27T09:00:00Z").copy(type = TransactionType.INCOME),
            expense(Pharmacy, null).copy(dueDate = Instant.parse("2026-12-01T10:00:00Z")),
            expense(null, "2026-11-02T10:00:00Z"),
        )
    }.flatten()

    /** The expenses' categories used most since [Since], at most [Limit], the most first. */
    val MostUsedExpenses = listOf(Groceries, Coffee, Restaurant, Transport)

    private fun category(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())

    private fun expense(category: UUID?, dateTime: String?) = TransactionEntity(
        accountId = UUID.nameUUIDFromBytes("Bank".toByteArray()),
        type = TransactionType.EXPENSE,
        amount = 10.0,
        dateTime = dateTime?.let(Instant::parse),
        categoryId = category,
    )
}
