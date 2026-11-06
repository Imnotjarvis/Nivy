package com.ivy.data.db.dao

import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.TransactionEntity
import org.jetbrains.annotations.VisibleForTesting
import java.time.Instant
import java.util.UUID

/**
 * The cases every TransactionDao.balance must pass: the database's query (TransactionDaoBalanceTest,
 * on a device) and FakeTransactionDao's copy of it (FakeTransactionDaoBalanceTest), so that the two
 * can't drift apart. The balance is what the transactions add up to when read one by one
 * (TransactionMapper, AccountValueFunctions.balance), what can't be read counting for nothing.
 */
@VisibleForTesting
object BalanceCases {
    val Bank = AccountEntity(name = "Bank", currency = "EUR", color = 0)
    val Cash = AccountEntity(name = "Cash", currency = "EUR", color = 0)
    val Card = AccountEntity(name = "Card", currency = "EUR", color = 0)

    /** An account that isn't there: deleted, or never saved. */
    private val Gone = UUID.randomUUID()

    // Before the transactions, which are dated from it.
    private val Start: Instant = Instant.parse("2026-10-01T10:00:00Z")
    private const val SecondsInDay = 86_400L

    val accounts = listOf(Bank, Cash, Card)

    val transactions = listOf(
        income(Bank.id, 1_000.0, day = 1),
        expense(Bank.id, 40.0, day = 2),
        transfer(Bank.id, 300.0, to = Cash.id, day = 3),
        // Received less: a different currency, or a fee.
        transfer(Bank.id, 200.0, to = Card.id, toAmount = 180.0, day = 4),
        // An invalid received amount is taken as what was sent.
        transfer(Cash.id, 30.0, to = Bank.id, toAmount = 0.0, day = 11),
        income(Cash.id, 15.0, day = 12),
        // An income with a receiving account counts there too, as it always has.
        income(Bank.id, 5.0, day = 13).copy(toAccountId = Card.id),
        // What can't be read as a transaction counts for nothing: a transfer with an account
        // that isn't there, a transfer to itself, an amount that isn't positive and finite.
        transfer(Bank.id, 50.0, to = Gone, day = 5),
        transfer(Gone, 70.0, to = Bank.id, day = 6),
        transfer(Bank.id, 25.0, to = Bank.id, day = 7),
        expense(Bank.id, 0.0, day = 8),
        expense(Bank.id, Double.POSITIVE_INFINITY, day = 10),
        // Not in the range asked: planned, and dated after its end.
        expense(Bank.id, 500.0, day = 9).copy(dateTime = null, dueDate = day(9)),
        expense(Bank.id, 60.0, day = 40),
    )

    /** What the balance of [account] from [start] to [end] must be. */
    data class Case(
        val description: String,
        val account: UUID,
        val start: Instant,
        val end: Instant,
        val balance: Double,
    )

    val cases = listOf(
        Case("Bank: 1,000 - 40 - 300 - 200 + 30 + 5", Bank.id, day(0), day(31), 495.0),
        Case("Cash: 300 - 30 + 15", Cash.id, day(0), day(31), 285.0),
        Case("Card: 180 + 5", Card.id, day(0), day(31), 185.0),
        Case("Bank on the 3rd and the 4th: its two transfers out", Bank.id, day(3), day(4), -500.0),
        Case("an account without transactions", UUID.randomUUID(), day(0), day(31), 0.0),
    )

    /** How far a balance may be off: half a cent. */
    const val Cents = 0.005

    private fun income(account: UUID, amount: Double, day: Long) =
        TransactionEntity(accountId = account, type = TransactionType.INCOME, amount = amount, dateTime = day(day))

    private fun expense(account: UUID, amount: Double, day: Long) =
        TransactionEntity(accountId = account, type = TransactionType.EXPENSE, amount = amount, dateTime = day(day))

    private fun transfer(from: UUID, amount: Double, to: UUID, day: Long, toAmount: Double? = null) =
        TransactionEntity(
            accountId = from,
            type = TransactionType.TRANSFER,
            amount = amount,
            toAccountId = to,
            toAmount = toAmount,
            dateTime = day(day),
        )

    private fun day(day: Long): Instant = Start.plusSeconds(day * SecondsInDay)
}
