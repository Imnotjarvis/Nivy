package com.nivy.transaction

import com.nivy.base.Toaster
import com.nivy.base.model.TransactionType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.model.Account
import com.nivy.data.repository.TransactionRepository
import com.nivy.data.repository.mapper.TransactionMapper
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.loan.LoanTransactionsUseCase
import com.nivy.domain.usecase.loan.NoLoanExchangeRate
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.ui.R
import com.nivy.wallet.domain.data.CustomExchangeRateState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/** Checks and saves what the editor entered, a copy of it, or the payment of a planned one. */
class TransactionSaving @Inject constructor(
    private val toaster: Toaster,
    private val transactionRepo: TransactionRepository,
    private val transactionMapper: TransactionMapper,
    private val loanTransactionsUseCase: LoanTransactionsUseCase,
    private val creditCardChargeUseCase: CreditCardChargeUseCase,
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val dispatchers: DispatchersProvider,
    private val timeProvider: TimeProvider,
    private val databaseTransaction: DatabaseTransaction,
) {
    fun validTransaction(
        scope: CoroutineScope,
        transactionType: TransactionType,
        account: Account?,
        toAccount: Account?,
        amount: Double,
        customExchangeRateState: CustomExchangeRateState,
    ): Boolean {
        if (hasChosenSameSourceAndDestinationAccountToTransfer(transactionType, account, toAccount)) {
            scope.launch {
                toaster.show(R.string.msg_source_account_destination_account_same_for_transfer)
            }
            return false
        }
        if (hasNotChosenAccountToTransfer(transactionType, toAccount)) {
            scope.launch {
                toaster.show(R.string.msg_select_account_to_transfer)
            }
            return false
        }

        if (amount <= 0.0) {
            return false
        }
        if (transactionType == TransactionType.TRANSFER && customExchangeRateState.showCard &&
            customExchangeRateState.exchangeRate == null
        ) {
            scope.launch {
                toaster.show(R.string.msg_enter_exchange_rate)
            }
            return false
        }

        return true
    }

    private fun hasNotChosenAccountToTransfer(transactionType: TransactionType, toAccount: Account?): Boolean {
        return transactionType == TransactionType.TRANSFER && toAccount == null
    }

    private fun hasChosenSameSourceAndDestinationAccountToTransfer(
        transactionType: TransactionType,
        account: Account?,
        toAccount: Account?,
    ): Boolean {
        return transactionType == TransactionType.TRANSFER && toAccount == account
    }

    /**
     * Runs [save] in the background; it returns false when it can't save. Then, or when it fails,
     * the screen says so and stays open with what was entered, instead of closing as if saved.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun trySaving(save: suspend () -> Boolean): Boolean {
        val saved = try {
            withContext(dispatchers.io) { save() }
        } catch (e: CancellationException) {
            throw e
        } catch (ignored: NoLoanExchangeRate) {
            toaster.show(R.string.msg_no_loan_exchange_rate)
            return false
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
        if (!saved) toaster.show(R.string.msg_transaction_not_saved)
        return saved
    }

    /**
     * Saves [draft] on its card's charge date, after its loan, which [loanUpdating] is told about,
     * then runs [linkTags], in one database transaction; false when it isn't a valid transaction.
     */
    internal suspend fun save(
        draft: TransactionDraft,
        linkTags: suspend () -> Unit,
        loanUpdating: (Boolean) -> Unit,
    ): Boolean {
        val transaction = draft.toDomain(transactionMapper) ?: return false
        databaseTransaction.run {
            if (transaction.metadata.loanId != null) {
                loanUpdating(true)
                try {
                    loanTransactionsUseCase.updateFromTransaction(transaction)
                } finally {
                    loanUpdating(false)
                }
            }
            transactionRepo.save(creditCardChargeUseCase.dated(transaction))
            linkTags()
        }
        return true
    }

    /** Saves a copy of [draft] as [id], dated now, then [copyTags]; false when it isn't a valid one. */
    internal suspend fun saveCopy(draft: TransactionDraft, id: UUID, copyTags: suspend () -> Unit): Boolean {
        val copy = draft
            .copy(
                id = id,
                dateTime = timeProvider.utcNow(),
                // Not another payment of the planned rule, nor part of the loan.
                dueDate = null,
                recurringRuleId = null,
                paidFor = null,
                loanId = null,
                loanRecordId = null,
            )
            .toDomain(transactionMapper)
            ?: return false
        // One database transaction: the copy isn't left without its tags.
        databaseTransaction.run {
            transactionRepo.save(creditCardChargeUseCase.dated(copy))
            copyTags()
        }
        return true
    }

    /** Pays the planned [draft]: the paid transaction, or null when there's nothing to pay. */
    internal suspend fun pay(draft: TransactionDraft): TransactionDraft? =
        draft.toDomain(transactionMapper)
            ?.let { dueTransactionsUseCase.pay(it) }
            ?.toDraft()
}
