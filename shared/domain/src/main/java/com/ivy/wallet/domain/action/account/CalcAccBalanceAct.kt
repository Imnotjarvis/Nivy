package com.ivy.wallet.domain.action.account

import com.ivy.base.model.AccountType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.repository.AccountValueRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An account's balance: what its transactions add up to. An investment account's is its value
 * instead, once one was entered: the last value with its cash, and the money moved in or out since.
 */
class CalcAccBalanceAct @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountValueRepository: AccountValueRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(input: Input): Output = withContext(Dispatchers.IO) {
        val range = input.range ?: ClosedTimeRange.allTimeIvy(timeProvider)
        val balance = balanceOf(input.account, range.from, range.to)
        val last = if (input.account.type == AccountType.INVESTMENT) {
            accountValueRepository.findByAccount(input.account.id).lastOrNull { it.time <= range.to }
        } else {
            null
        }
        if (last == null) return@withContext Output(account = input.account, balance = balance)

        val putInThen = balanceOf(input.account, range.from, last.time)
        val gain = last.total.toBigDecimal() - putInThen
        // Money moved in since waits as cash, until it's invested; money taken out comes from the
        // cash, and from the investments once it's spent.
        val cash = last.cash?.let { (it.value.toBigDecimal() + balance - putInThen).max(BigDecimal.ZERO) }
        Output(account = input.account, balance = balance + gain, gain = gain, cash = cash)
    }

    // What the account's transactions from [from] to [to] add up to, summed by the database.
    private suspend fun balanceOf(account: Account, from: Instant, to: Instant): BigDecimal =
        transactionRepository.balance(accountId = account.id, startDate = from, endDate = to).toBigDecimal()

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
