package com.nivy.transactions

import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.AccountValue
import com.nivy.data.model.Transfer
import com.nivy.data.repository.AccountValueRepository
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.account.AccountTransactionsUseCase
import com.nivy.domain.usecase.account.invested
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.data.model.toCloseTimeRange
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.transaction.AccountValueFunctions
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/** An investment account's money in, gain, values and value chart, as its page shows them. */
class AccountInvestmentData @Inject constructor(
    private val periodState: PeriodState,
    private val accountTransactionsUseCase: AccountTransactionsUseCase,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val accountValueRepository: AccountValueRepository,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) {
    // What [account]'s transactions put in and invested, at each value entered too, and the transfers into it.
    suspend fun investmentData(
        account: Account,
        balance: Double,
        gain: Double?,
        cash: Double?,
        valuesExpanded: Boolean,
    ): InvestmentData {
        val transactions = accountTransactionsUseCase.transactions(
            accountId = account.id.value,
            range = ClosedTimeRange.allTimeNivy(timeProvider)
        )
        val values = accountValueRepository.findByAccount(account.id).reversed().map { value ->
            ValueEntry(
                value = value,
                invested = transactions.filter { it.time <= value.time }
                    .sumOf { AccountValueFunctions.balance(it, account.id.value) }
                    .toDouble() - (value.cash?.value ?: 0.0),
            )
        }
        return InvestmentData(
            chart = valueChart(account, firstValue = values.lastOrNull()?.value),
            invested = invested(balance, gain, cash),
            gain = gain,
            cash = cash,
            transfersIn = transactions.count {
                it is Transfer && it.toAccount == account.id && it.fromAccount != account.id
            },
            values = values.toImmutableList(),
            valuesExpanded = valuesExpanded,
        )
    }

    // The value and the money invested at the end of each of the last months, as the balance has them
    // then: from the month of the first value, before which there's no value to show.
    private suspend fun valueChart(account: Account, firstValue: AccountValue?): ImmutableList<ValuePoint> {
        val startDay = periodState.startDayOfMonth
        val current = TimePeriod.currentMonth(startDayOfMonth = startDay, timeProvider = timeProvider)
        val month = current.month
        val year = current.year
        if (firstValue == null || month == null || year == null) return persistentListOf()
        val now = timeProvider.utcNow()
        return (ChartMonths - 1 downTo 0).mapNotNull { monthsAgo ->
            val period = month.incrementMonthPeriod(-monthsAgo.toLong(), year)
            val end = minOf(period.toRange(startDay, timeConverter, timeProvider).toCloseTimeRange().to, now)
            if (end < firstValue.time) return@mapNotNull null
            val output = accountBalanceUseCase.balance(account = account, range = ClosedTimeRange.to(end))
            val value = output.balance.toDouble()
            ValuePoint(
                month = period.month,
                value = value,
                invested = invested(value, output.gain?.toDouble(), output.cash?.toDouble()),
            )
        }.toImmutableList()
    }
}

private const val ChartMonths = 12
