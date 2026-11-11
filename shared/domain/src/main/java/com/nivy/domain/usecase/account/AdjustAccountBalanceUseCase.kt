package com.nivy.domain.usecase.account

import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/** Adds the income or expense that brings an account's balance to a new value. */
class AdjustAccountBalanceUseCase @Inject constructor(
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider,
) {
    /**
     * @param currentBalance the account's balance now; calculated when it isn't given.
     */
    suspend fun adjust(
        account: Account,
        newBalance: Double,
        currentBalance: Double? = null,
    ) {
        val balance = currentBalance
            ?: accountBalanceUseCase.balance(account = account).balance.toDouble()
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
            purchaseDateTime = null,
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
                    value = value,
                    account = account.id,
                )
            }
        )
    }

    companion object {
        /** The title of a transaction that adjusts a balance: the Cashew import gives it to Cashew's too. */
        const val AdjustTitle = "Adjust balance"
    }
}
