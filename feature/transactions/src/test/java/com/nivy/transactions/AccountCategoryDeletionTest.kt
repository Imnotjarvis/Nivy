package com.nivy.transactions

import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.AccountValueRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.navigation.Navigation
import io.mockk.confirmVerified
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class AccountCategoryDeletionTest {

    private val accountRepository = mockk<AccountRepository>(relaxUnitFun = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxUnitFun = true)
    private val transactionRepository = mockk<TransactionRepository>(relaxUnitFun = true)
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
        // given
        val id = UUID.randomUUID()

        // when
        deletion.deleteAccount(id)

        // then
        coVerifySequence {
            transactionRepository.deleteAllByAccountId(AccountId(id))
            plannedPaymentRuleRepository.deleteAllByAccountId(AccountId(id))
            creditCardRepository.deleteAllByAccountId(AccountId(id))
            accountValueRepository.deleteAllByAccountId(AccountId(id))
            accountRepository.deleteById(AccountId(id))
            nav.back()
        }
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
}
