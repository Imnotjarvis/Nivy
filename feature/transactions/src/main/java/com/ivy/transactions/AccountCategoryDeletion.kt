package com.ivy.transactions

import com.ivy.data.model.AccountId
import com.ivy.data.model.CategoryId
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.AccountValueRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.PlannedPaymentRuleRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.navigation.Navigation
import com.ivy.wallet.utils.ioThread
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
