package com.ivy.wallet.domain.action.category

import com.ivy.base.legacy.Transaction
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryTrnsBetweenAct @Inject constructor(
    private val transactionDao: TransactionDao
) {
    suspend operator fun invoke(input: Input): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.findAllByCategoryAndBetween(
            startDate = input.between.from,
            endDate = input.between.to,
            categoryId = input.categoryId
        ).map { it.toLegacyDomain() }
    }

    data class Input(
        val categoryId: UUID,
        val between: ClosedTimeRange
    )
}
