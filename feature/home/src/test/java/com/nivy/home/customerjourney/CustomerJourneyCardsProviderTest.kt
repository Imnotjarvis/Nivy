package com.nivy.home.customerjourney

import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CustomerJourneyCardsProviderTest {
    private val transactions = mockk<TransactionRepository>()
    private val plannedPayments = mockk<PlannedPaymentRuleRepository>()
    private val appPreferences = mockk<AppPreferences>(relaxed = true) {
        coEvery { isCardDismissed(any()) } returns false
    }
    private val provider = CustomerJourneyCardsProvider(
        transactionRepository = transactions,
        plannedPaymentRuleRepository = plannedPayments,
        appPreferences = appPreferences,
    )

    @Test
    fun `a wallet with no transaction is asked for its balance, and nothing else`() = runTest {
        cardsFor(transactions = 0, plannedPayments = 0) shouldBe listOf("adjust_balance")
    }

    @Test
    fun `a planned payment is suggested from the first transaction, until there is one`() = runTest {
        cardsFor(transactions = 1, plannedPayments = 0) shouldBe listOf("add_planned_payment")
        cardsFor(transactions = 1, plannedPayments = 1) shouldBe emptyList()
    }

    @Test
    fun `the widget is suggested from the third transaction, the pie chart from the seventh`() = runTest {
        cardsFor(transactions = 2, plannedPayments = 1) shouldBe emptyList()
        cardsFor(transactions = 3, plannedPayments = 1) shouldBe listOf("add_transaction_widget")
        cardsFor(transactions = 6, plannedPayments = 1) shouldBe listOf("add_transaction_widget")
        cardsFor(transactions = 7, plannedPayments = 1) shouldBe
            listOf("add_transaction_widget", "expenses_pie_chart")
    }

    @Test
    fun `a dismissed card stays away, the others stay`() = runTest {
        coEvery { appPreferences.isCardDismissed("add_transaction_widget") } returns true

        cardsFor(transactions = 7, plannedPayments = 0) shouldBe listOf("add_planned_payment", "expenses_pie_chart")
    }

    @Test
    fun `dismissing a card remembers it by its id`() = runTest {
        provider.dismissCard(CustomerJourneyCardsProvider.didYouKnowExpensesPieChart())

        coVerify(exactly = 1) { appPreferences.setCardDismissed("expenses_pie_chart") }
    }

    private suspend fun cardsFor(transactions: Long, plannedPayments: Long): List<String> {
        coEvery { this@CustomerJourneyCardsProviderTest.transactions.countHappenedTransactions() } returns transactions
        coEvery { this@CustomerJourneyCardsProviderTest.plannedPayments.count() } returns plannedPayments
        return provider.loadCards().map { it.id }
    }
}
