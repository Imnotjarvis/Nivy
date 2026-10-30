package com.ivy.wallet.domain.action.transaction

import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrnByIdAct @Inject constructor(
    private val transactionRepo: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: UUID): Transaction? = withContext(Dispatchers.IO) {
        transactionRepo.findById(TransactionId(transactionId))
    }
}
