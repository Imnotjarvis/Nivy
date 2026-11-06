package com.ivy.search

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.ui.testing.ComposeViewModelTest
import com.ivy.ui.testing.runTest
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.transaction.TrnsWithDateDivsAct
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class SearchViewModelTest : ComposeViewModelTest() {
    private val coffee = expense(title = "Morning coffee")
    private val rent = expense(title = "Rent")

    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { findAll() } returns listOf(coffee, rent)
    }
    private val searched = slot<TrnsWithDateDivsAct.Input>()

    // What the screen lists for what's found, whatever it is.
    private val results = listOf(TransactionHistoryDateDivider(LocalDate.of(2023, 11, 1), 0.0, 0.0))

    private val viewModel = SearchViewModel(
        trnsWithDateDivsAct = mockk<TrnsWithDateDivsAct> {
            coEvery { this@mockk(capture(searched)) } returns results
        },
        accountsAct = mockk<AccountsAct> { coEvery { this@mockk() } returns persistentListOf() },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
        baseCurrencyAct = mockk<BaseCurrencyAct> { coEvery { this@mockk() } returns "EUR" },
        transactionRepository = transactionRepository,
        features = mockk(relaxed = true),
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `nothing is listed before anything is typed`() {
        viewModel.runTest {
            transactions.shouldBeEmpty()
        }
        coVerify(exactly = 0) { transactionRepository.findAll() }
    }

    @Test
    fun `a query lists the transactions it matches`() {
        viewModel.runTest(events = listOf(SearchEvent.Search("coffee"))) {
            searchQuery shouldBe "coffee"
            transactions shouldBe results
        }
        searched.captured.transactions shouldBe listOf(coffee)
    }

    @Test
    fun `clearing the query lists nothing again`() {
        viewModel.runTest(events = listOf(SearchEvent.Search("coffee"), SearchEvent.Search(""))) {
            searchQuery shouldBe ""
            transactions.shouldBeEmpty()
        }
    }

    private fun expense(title: String) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = NotBlankTrimmedString.unsafe(title),
        description = null,
        category = null,
        time = Instant.parse("2023-11-01T10:00:00Z"),
        settled = true,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        ),
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(10.0), AssetCode.unsafe("EUR")),
        account = AccountId(UUID.randomUUID()),
    )
}
