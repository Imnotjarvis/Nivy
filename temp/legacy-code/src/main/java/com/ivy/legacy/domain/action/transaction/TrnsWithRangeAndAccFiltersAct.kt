package com.ivy.wallet.domain.action.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrnsWithRangeAndAccFiltersAct @Inject constructor(
    private val transactionDao: TransactionDao
) {
    suspend operator fun invoke(input: Input): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.findAllBetween(input.range.from(), input.range.to())
            .map { it.toLegacyDomain() }
            .filter {
                input.accountIdFilterSet.contains(it.accountId) ||
                    input.accountIdFilterSet.contains(it.toAccountId)
            }
    }

    data class Input(
        val range: com.ivy.legacy.data.model.FromToTimeRange,
        val accountIdFilterSet: Set<UUID>
    )
}
