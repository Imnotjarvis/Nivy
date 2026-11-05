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
 * instead, once one was entered: the last value with its cash, and the money moved in or out since.
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
        val last = if (input.account.type == AccountType.INVESTMENT) {
            accountValueRepository.findByAccount(input.account.id).lastOrNull { it.time <= range.to }
        } else {
            null
        }
        if (last == null) return@withContext Output(account = input.account, balance = balance)

        val putInThen = balanceOf(accTrns.filter { it.time <= last.time }, input.account)
        val gain = last.total.toBigDecimal() - putInThen
        // Money moved in since waits as cash, until it's invested; money taken out comes from the
        // cash, and from the investments once it's spent.
        val cash = last.cash?.let { (it.value.toBigDecimal() + balance - putInThen).max(BigDecimal.ZERO) }
        Output(account = input.account, balance = balance + gain, gain = gain, cash = cash)
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
     * negative) it's worth than the money put in, which is [balance] less it.
     * @property cash of that money, what isn't invested, when the last value has its cash.
     */
    @Suppress("DataClassDefaultValues")
    data class Output(
        val account: Account,
        val balance: BigDecimal,
        val gain: BigDecimal? = null,
        val cash: BigDecimal? = null,
    )
}
