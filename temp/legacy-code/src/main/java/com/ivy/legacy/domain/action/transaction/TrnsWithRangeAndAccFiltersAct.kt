package com.ivy.wallet.domain.action.transaction

import com.ivy.data.model.Transaction
import com.ivy.data.model.getFromAccount
import com.ivy.data.model.getToAccount
import com.ivy.data.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrnsWithRangeAndAccFiltersAct @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(input: Input): List<Transaction> = withContext(Dispatchers.IO) {
        transactionRepository.findAllBetween(input.range.from(), input.range.to())
            .filter {
                input.accountIdFilterSet.contains(it.getFromAccount().value) ||
                    input.accountIdFilterSet.contains(it.getToAccount()?.value)
            }
    }

    data class Input(
        val range: com.ivy.legacy.data.model.FromToTimeRange,
        val accountIdFilterSet: Set<UUID>
    )
}
