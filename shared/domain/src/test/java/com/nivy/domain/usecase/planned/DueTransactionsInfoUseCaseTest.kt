package com.nivy.domain.usecase.planned

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.TransactionType
import com.nivy.domain.usecase.HomeNumbers
import com.nivy.domain.usecase.HomeNumbers.Companion.Euros
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov1
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov30
import com.nivy.domain.usecase.HomeNumbers.Companion.Today
import com.nivy.domain.usecase.HomeNumbers.Companion.timeConverter
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

/** What's due in a range, as Home's upcoming and overdue cards add it up. */
class DueTransactionsInfoUseCaseTest {
    private val home = HomeNumbers()
    private val useCase = DueTransactionsInfoUseCase(
        transactionRepository = home.transactions,
        exchangeUseCase = home.exchange,
        timeProvider = Today,
        timeConverter = timeConverter,
        dispatchers = TestDispatchersProvider,
    )
    private val november = ClosedTimeRange(from = Nov1, to = Nov30)

    @Test
    fun `the payments due in the range that pass the filter, in the base currency`() = runTest {
        val bank = home.account("Bank")
        val dollars = home.account("Dollars", currency = "USD")
        home.rate("USD", perEuro = 1.1)
        home.planned(bank, TransactionType.EXPENSE, 50.0, due = Nov20)
        home.planned(dollars, TransactionType.EXPENSE, 11.0, due = Nov25)
        home.planned(bank, TransactionType.INCOME, 1_500.0, due = Nov28)
        // Overdue, so not upcoming; and paid, so not due at all.
        home.planned(bank, TransactionType.EXPENSE, 30.0, due = Nov5)
        home.expense(bank, 99.0)

        val upcoming = useCase.info(november, Euros) { due, today -> due.isAfter(today) }

        upcoming.dueTrns.size shouldBe 3
        upcoming.dueIncomeExpense.expense.toDouble() shouldBe (60.0 plusOrMinus Cent)
        upcoming.dueIncomeExpense.income.toDouble() shouldBe (1_500.0 plusOrMinus Cent)
    }

    @Test
    fun `a payment due in a currency without a rate is listed, and counts as nothing`() = runTest {
        val bank = home.account("Bank")
        val pounds = home.account("Pounds", currency = "GBP")
        home.planned(bank, TransactionType.EXPENSE, 50.0, due = Nov20)
        home.planned(pounds, TransactionType.EXPENSE, 80.0, due = Nov25)

        val upcoming = useCase.info(november, Euros) { due, today -> due.isAfter(today) }

        upcoming.dueTrns.size shouldBe 2
        upcoming.dueIncomeExpense.expense.toDouble() shouldBe (50.0 plusOrMinus Cent)
    }

    private companion object {
        const val Cent = 0.005
        val Nov5: Instant = Instant.parse("2026-11-05T09:00:00Z")
        val Nov20: Instant = Instant.parse("2026-11-20T09:00:00Z")
        val Nov25: Instant = Instant.parse("2026-11-25T09:00:00Z")
        val Nov28: Instant = Instant.parse("2026-11-28T09:00:00Z")
    }
}
