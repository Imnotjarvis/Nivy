package com.ivy.wallet.domain.action.account

import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/** Adds the income or expense that brings an account's balance to a new value. */
class AdjustAccBalanceAct @Inject constructor(
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider,
) {
    /**
     * @param currentBalance the account's balance now; calculated when it isn't given.
     */
    suspend operator fun invoke(
        account: Account,
        newBalance: Double,
        currentBalance: Double? = null,
    ) {
        val balance = currentBalance
            ?: calcAccBalanceAct(CalcAccBalanceAct.Input(account = account)).balance.toDouble()
        val diff = balance - newBalance
        val value = PositiveDouble.from(abs(diff)).getOrNull()
            ?.let { PositiveValue(amount = it, asset = account.asset) }
            ?: return

        val id = TransactionId(UUID.randomUUID())
        val title = NotBlankTrimmedString.unsafe(AdjustTitle)
        val time = timeProvider.utcNow()
        val metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
        )
        transactionRepository.save(
            if (diff < 0) {
                Income(
                    id = id,
                    title = title,
                    description = null,
                    category = null,
                    time = time,
                    settled = true,
                    metadata = metadata,
                    tags = emptyList(),
                    value = value,
                    account = account.id,
                )
            } else {
                Expense(
                    id = id,
                    title = title,
                    description = null,
                    category = null,
                    time = time,
                    settled = true,
                    metadata = metadata,
                    tags = emptyList(),
                    value = value,
                    account = account.id,
                )
            }
        )
    }

    private companion object {
        const val AdjustTitle = "Adjust balance"
    }
}
