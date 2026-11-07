package com.nivy.domain.usecase.account

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.AccountId
import com.nivy.data.model.Transaction
import com.nivy.data.repository.TransactionRepository
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/** An account's transactions in a range, with the transfers into it. */
class AccountTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun transactions(accountId: UUID, range: ClosedTimeRange): List<Transaction> = withContext(dispatchers.io) {
        transactionRepository.findAllByAccountAndBetween(
            accountId = AccountId(accountId),
            startDate = range.from,
            endDate = range.to
        ) + transactionRepository.findAllToAccountAndBetween(
            toAccountId = AccountId(accountId),
            startDate = range.from,
            endDate = range.to
        )
    }
}
