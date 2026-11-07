package com.nivy.transactions

import com.nivy.base.model.AccountType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.AccountValueRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.navigation.Navigation
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class AccountCategoryDeletionTest {

    private val oldBank = account("Old bank", Eur)
    private val revolut = account("Revolut", Usd)
    private val cash = account("Cash", Usd)

    private var transactions = listOf<Transaction>()
    private val saved = mutableListOf<Transaction>()

    private val accountRepository = mockk<AccountRepository>(relaxUnitFun = true) {
        coEvery { findById(oldBank.id) } returns oldBank
    }
    private val categoryRepository = mockk<CategoryRepository>(relaxUnitFun = true)
    private val transactionRepository = mockk<TransactionRepository>(relaxUnitFun = true) {
        coEvery { findAll() } answers { transactions }
        coEvery { save(capture(saved)) } returns Unit
    }
    private val plannedPaymentRuleRepository = mockk<PlannedPaymentRuleRepository>(relaxUnitFun = true)
    private val creditCardRepository = mockk<CreditCardRepository>(relaxUnitFun = true)
    private val accountValueRepository = mockk<AccountValueRepository>(relaxUnitFun = true)
    private val nav = mockk<Navigation>(relaxed = true)

    private val deletion = AccountCategoryDeletion(
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        transactionRepository = transactionRepository,
        plannedPaymentRuleRepository = plannedPaymentRuleRepository,
        creditCardRepository = creditCardRepository,
        accountValueRepository = accountValueRepository,
        nav = nav,
    )

    @Test
    fun `deleting an account deletes what's on it, then the account, and leaves its page`() = runTest {
        // when
        deletion.deleteAccount(oldBank.id.value)

        // then
        coVerifySequence {
            accountRepository.findById(oldBank.id)
            transactionRepository.findAll()
            transactionRepository.deleteAllByAccountId(oldBank.id)
            plannedPaymentRuleRepository.deleteAllByAccountId(oldBank.id)
            creditCardRepository.deleteAllByAccountId(oldBank.id)
            accountValueRepository.deleteAllByAccountId(oldBank.id)
            accountRepository.deleteById(oldBank.id)
            nav.back()
        }
    }

    @Test
    fun `its transfers with other accounts stay on them, as expenses and incomes noting the deleted account`() =
        runTest {
            // given
            val savings = transfer(revolut, 60.0, oldBank, 50.0, description = "Savings")
            val refill = transfer(oldBank, 20.0, revolut, 24.0)
            val unrelated = transfer(revolut, 10.0, cash, 10.0)
            transactions = listOf(savings, refill, unrelated)

            // when
            deletion.deleteAccount(oldBank.id.value)

            // then
            saved shouldBe listOf(
                Expense(
                    id = savings.id,
                    title = null,
                    description = NotBlankTrimmedString.unsafe("Savings\nTransfer to Old bank, a deleted account"),
                    category = null,
                    time = savings.time,
                    settled = true,
                    metadata = Metadata,
                    tags = emptyList(),
                    value = savings.fromValue,
                    account = revolut.id,
                ),
                Income(
                    id = refill.id,
                    title = null,
                    description = NotBlankTrimmedString.unsafe("Transfer from Old bank, a deleted account"),
                    category = null,
                    time = refill.time,
                    settled = true,
                    metadata = Metadata,
                    tags = emptyList(),
                    value = refill.toValue,
                    account = revolut.id,
                ),
            )
            coVerifyOrder {
                transactionRepository.save(any())
                transactionRepository.save(any())
                transactionRepository.deleteAllByAccountId(oldBank.id)
            }
        }

    @Test
    fun `deleting an account that's already gone keeps no transfer`() = runTest {
        // given
        val gone = AccountId(UUID.randomUUID())
        coEvery { accountRepository.findById(gone) } returns null

        // when
        deletion.deleteAccount(gone.value)

        // then
        coVerify(exactly = 0) { transactionRepository.save(any()) }
        coVerify(exactly = 1) { accountRepository.deleteById(gone) }
    }

    @Test
    fun `deleting a category keeps its transactions, and leaves its page`() = runTest {
        // given
        val id = UUID.randomUUID()

        // when
        deletion.deleteCategory(id)

        // then
        coVerifyOrder {
            categoryRepository.deleteById(CategoryId(id))
            nav.back()
        }
        confirmVerified(transactionRepository)
    }

    private fun transfer(
        from: Account,
        sent: Double,
        to: Account,
        received: Double,
        description: String? = null,
    ) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = description?.let(NotBlankTrimmedString::unsafe),
        category = null,
        time = Instant.parse("2026-11-03T10:00:00Z"),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        fromAccount = from.id,
        fromValue = PositiveValue(PositiveDouble.unsafe(sent), from.asset),
        toAccount = to.id,
        toValue = PositiveValue(PositiveDouble.unsafe(received), to.asset),
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")

        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

        fun account(name: String, asset: AssetCode) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )
    }
}
