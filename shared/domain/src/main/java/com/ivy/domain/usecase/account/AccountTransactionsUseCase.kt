package com.ivy.domain.usecase.account

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.AccountId
import com.ivy.data.model.Transaction
import com.ivy.data.repository.TransactionRepository
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
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
