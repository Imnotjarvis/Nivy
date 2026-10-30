package com.ivy.wallet.domain.action.account

import com.ivy.data.model.AccountId
import com.ivy.data.model.Transaction
import com.ivy.data.repository.TransactionRepository
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccTrnsAct @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(input: Input): List<Transaction> = withContext(Dispatchers.IO) {
        transactionRepository.findAllByAccountAndBetween(
            accountId = AccountId(input.accountId),
            startDate = input.range.from,
            endDate = input.range.to
        ) + transactionRepository.findAllToAccountAndBetween(
            toAccountId = AccountId(input.accountId),
            startDate = input.range.from,
            endDate = input.range.to
        )
    }

    class Input(
        val accountId: UUID,
        val range: ClosedTimeRange
    )
}
