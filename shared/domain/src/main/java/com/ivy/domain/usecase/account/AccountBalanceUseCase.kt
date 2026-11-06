package com.ivy.domain.usecase.account

import com.ivy.base.model.AccountType
import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.repository.AccountValueRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

/**
 * An account's balance: what its transactions add up to. An investment account's is its value
 * instead, once one was entered: the last value with its cash, and the money moved in or out since.
 */
class AccountBalanceUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountValueRepository: AccountValueRepository,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun balance(
        account: Account,
        range: ClosedTimeRange? = null,
    ): AccountBalanceDetails = withContext(dispatchers.io) {
        val timeRange = range ?: ClosedTimeRange.allTimeIvy(timeProvider)
        val balance = balanceOf(account, timeRange.from, timeRange.to)
        val last = if (account.type == AccountType.INVESTMENT) {
            accountValueRepository.findByAccount(account.id).lastOrNull { it.time <= timeRange.to }
        } else {
            null
        }
        if (last == null) return@withContext AccountBalanceDetails(account = account, balance = balance)

        val putInThen = balanceOf(account, timeRange.from, last.time)
        val gain = last.total.toBigDecimal() - putInThen
        // Money moved in since waits as cash, until it's invested; money taken out comes from the
        // cash, and from the investments once it's spent.
        val cash = last.cash?.let { (it.value.toBigDecimal() + balance - putInThen).max(BigDecimal.ZERO) }
        AccountBalanceDetails(account = account, balance = balance + gain, gain = gain, cash = cash)
    }

    // What the account's transactions from [from] to [to] add up to, summed by the database.
    private suspend fun balanceOf(account: Account, from: Instant, to: Instant): BigDecimal =
        transactionRepository.balance(accountId = account.id, startDate = from, endDate = to).toBigDecimal()
}

/**
 * @property gain for an investment account with a value entered, how much more (or less,
 * negative) it's worth than the money put in, which is [balance] less it.
 * @property cash of that money, what isn't invested, when the last value has its cash.
 */
@Suppress("DataClassDefaultValues")
data class AccountBalanceDetails(
    val account: Account,
    val balance: BigDecimal,
    val gain: BigDecimal? = null,
    val cash: BigDecimal? = null,
)
