package com.nivy.transactions

import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.NotBlankTrimmedString
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
            keepTransfersWithOtherAccounts(AccountId(accountId))
            transactionRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            plannedPaymentRuleRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            creditCardRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            accountValueRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            accountRepository.deleteById(AccountId(accountId))

            nav.back()
        }
    }

    /**
     * The money a transfer with another account moved was paid all the same: the other account keeps
     * it, as an expense when it went to the deleted account and an income when it came from it, with
     * a note naming the deleted account. Left a transfer, it would count for neither account, and the
     * other account's balance would change.
     */
    private suspend fun keepTransfersWithOtherAccounts(deleted: AccountId) {
        val name = accountRepository.findById(deleted)?.name?.value ?: return
        transactionRepository.findAll()
            .filterIsInstance<Transfer>()
            .mapNotNull { transfer ->
                when (deleted) {
                    transfer.toAccount -> transfer.asExpense(note = "Transfer to $name, a deleted account")
                    transfer.fromAccount -> transfer.asIncome(note = "Transfer from $name, a deleted account")
                    else -> null
                }
            }
            .forEach { transactionRepository.save(it) }
    }

    suspend fun deleteCategory(categoryId: UUID) {
        ioThread {
            categoryRepository.deleteById(CategoryId(categoryId))

            nav.back()
        }
    }
}

private fun Transfer.asExpense(note: String): Transaction = Expense(
    id = id,
    title = title,
    description = description.withNote(note),
    category = category,
    time = time,
    settled = settled,
    metadata = metadata,
    tags = tags,
    value = fromValue,
    account = fromAccount,
)

private fun Transfer.asIncome(note: String): Transaction = Income(
    id = id,
    title = title,
    description = description.withNote(note),
    category = category,
    time = time,
    settled = settled,
    metadata = metadata,
    tags = tags,
    value = toValue,
    account = toAccount,
)

/** The description, with [note] on a line of its own after it. */
private fun NotBlankTrimmedString?.withNote(note: String): NotBlankTrimmedString =
    NotBlankTrimmedString.unsafe(listOfNotNull(this?.value, note).joinToString("\n"))
