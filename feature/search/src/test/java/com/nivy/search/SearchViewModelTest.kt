package com.nivy.search

import com.nivy.base.TestDispatchersProvider
import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.ui.testing.ComposeViewModelTest
import com.nivy.ui.testing.runTest
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
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
    private val searched = slot<List<Transaction>>()

    // What the screen lists for what's found, whatever it is.
    private val results = listOf(TransactionHistoryDateDivider(LocalDate.of(2023, 11, 1), 0.0, 0.0))

    private val viewModel = SearchViewModel(
        transactionsWithDateDividersUseCase = mockk<TransactionsWithDateDividersUseCase> {
            coEvery { withDateDividers(any(), capture(searched)) } returns results
        },
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns persistentListOf() },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
        currencyRepository = mockk<CurrencyRepository> {
            coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR")
        },
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
        searched.captured shouldBe listOf(coffee)
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
        value = PositiveValue(PositiveDouble.unsafe(10.0), AssetCode.unsafe("EUR")),
        account = AccountId(UUID.randomUUID()),
    )
}
