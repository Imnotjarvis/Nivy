package com.ivy.wallet.domain.action.account

import arrow.core.nonEmptyListOf
import com.ivy.base.model.AccountType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Transaction
import com.ivy.data.repository.AccountValueRepository
import com.ivy.wallet.domain.pure.transaction.AccountValueFunctions
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.transaction.foldTransactions
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An account's balance: what its transactions add up to. An investment account's is its value
 * instead, once one was entered: the last value, with the money moved in or out since then.
 */
class CalcAccBalanceAct @Inject constructor(
    private val accTrnsAct: AccTrnsAct,
    private val accountValueRepository: AccountValueRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(input: Input): Output = withContext(Dispatchers.IO) {
        val range = input.range ?: ClosedTimeRange.allTimeIvy(timeProvider)
        val accTrns = accTrnsAct(
            AccTrnsAct.Input(
                accountId = input.account.id.value,
                range = range
            )
        )
        val balance = balanceOf(accTrns, input.account)
        val gain = if (input.account.type == AccountType.INVESTMENT) {
            accountValueRepository.findByAccount(input.account.id)
                .lastOrNull { it.time <= range.to }
                ?.let { last ->
                    last.value.value.toBigDecimal() -
                        balanceOf(accTrns.filter { it.time <= last.time }, input.account)
                }
        } else {
            null
        }
        Output(account = input.account, balance = balance + (gain ?: BigDecimal.ZERO), gain = gain)
    }

    private fun balanceOf(transactions: List<Transaction>, account: Account): BigDecimal =
        foldTransactions(
            transactions = transactions,
            arg = account.id.value,
            valueFunctions = nonEmptyListOf(AccountValueFunctions::balance)
        ).head

    @Suppress("DataClassDefaultValues")
    data class Input(
        val account: Account,
        val range: ClosedTimeRange? = null
    )

    /**
     * @property gain for an investment account with a value entered, how much more (or less,
     * negative) it's worth than the money put in: [balance] is that money and this gain.
     */
    @Suppress("DataClassDefaultValues")
    data class Output(
        val account: Account,
        val balance: BigDecimal,
        val gain: BigDecimal? = null,
    )
}
