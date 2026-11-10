package com.nivy.domain.usecase.transaction

import com.nivy.base.TestDispatchersProvider
import com.nivy.data.model.getFromAccount
import com.nivy.domain.usecase.HomeNumbers
import com.nivy.domain.usecase.HomeNumbers.Companion.Euros
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov1
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov3
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov30
import com.nivy.domain.usecase.HomeNumbers.Companion.Oct20
import com.nivy.domain.usecase.HomeNumbers.Companion.timeConverter
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** The transactions of a range, and Home's list of them under a line per day. */
class TransactionListsTest {
    private val home = HomeNumbers()
    private val inRange = TransactionsInRangeUseCase(home.transactions, TestDispatchersProvider)
    private val withDividers = TransactionsWithDateDividersUseCase(
        exchangeUseCase = home.exchange,
        tagRepository = home.tags,
        timeConverter = timeConverter,
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `a range's transactions of the accounts asked, a transfer to one of them included`() = runTest {
        val bank = home.account("Bank")
        val cash = home.account("Cash")
        val card = home.account("Card")
        home.expense(bank, 40.0)
        home.transfer(card, bank, 100.0)
        home.expense(card, 15.0)
        home.expense(bank, 75.0, on = Oct20)

        val listed = inRange.transactions(ClosedTimeRange(Nov1, Nov30), accountIdFilterSet = setOf(bank.id.value))

        listed.size shouldBe 2
        listed.count { it.getFromAccount() == card.id } shouldBe 1
        listed.none { it.getFromAccount() == cash.id } shouldBe true
    }

    @Test
    fun `each day comes under a line with its income and expenses, the newest day first`() = runTest {
        val bank = home.account("Bank")
        val dollars = home.account("Dollars", currency = "USD")
        home.rate("USD", perEuro = 1.1)
        home.income(bank, 1_000.0, on = Nov3)
        home.expense(dollars, 22.0, on = Nov3)
        home.expense(bank, 8.0, on = Nov4)

        val items = withDividers.withDateDividers(Euros, home.transactions.findAll())

        items.size shouldBe 5
        val newest = items[0].shouldBeInstanceOf<TransactionHistoryDateDivider>()
        newest.date shouldBe LocalDate.of(2026, 11, 4)
        newest.expenses shouldBe (8.0 plusOrMinus Cent)
        items[1].shouldBeInstanceOf<TransactionHistoryEntry>()
        val third = items[2].shouldBeInstanceOf<TransactionHistoryDateDivider>()
        third.date shouldBe LocalDate.of(2026, 11, 3)
        third.income shouldBe (1_000.0 plusOrMinus Cent)
        third.expenses shouldBe (20.0 plusOrMinus Cent)
    }

    @Test
    fun `an account's list shows its transactions' tags, as Home's does`() = runTest {
        val bank = home.account("Bank")
        val lunch = home.expense(bank, 12.0)
        val trip = home.tag("Trip", lunch)
        val november = ClosedTimeRange(Nov1, Nov30)

        val onHome = withDividers.withDateDividers(Euros, home.transactions.findAllBetween(Nov1, Nov30))
        val onTheAccountsPage = withDividers.withDateDividers(
            Euros,
            home.accountTransactions.transactions(bank.id.value, november),
        )

        onHome.filterIsInstance<TransactionHistoryEntry>().single().tags shouldBe listOf(trip)
        onTheAccountsPage.filterIsInstance<TransactionHistoryEntry>().single().tags shouldBe listOf(trip)
    }

    private companion object {
        const val Cent = 0.005
        val Nov4: Instant = Instant.parse("2026-11-04T08:00:00Z")
    }
}
