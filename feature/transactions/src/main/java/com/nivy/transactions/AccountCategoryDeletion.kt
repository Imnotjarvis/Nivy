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
import com.nivy.wallet.utils.ioThread
import java.util.UUID
import javax.inject.Inject

/** Deletes the page's account, with everything on it, or its category, then goes back. */
class AccountCategoryDeletion @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
    private val creditCardRepository: CreditCardRepository,
    private val accountValueRepository: AccountValueRepository,
    private val nav: Navigation,
) {
    suspend fun deleteAccount(accountId: UUID) {
        ioThread {
            transactionRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            plannedPaymentRuleRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            creditCardRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            accountValueRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            accountRepository.deleteById(AccountId(accountId))

            nav.back()
        }
    }

    suspend fun deleteCategory(categoryId: UUID) {
        ioThread {
            categoryRepository.deleteById(CategoryId(categoryId))

            nav.back()
        }
    }
}
