package com.nivy.balance.detail

import com.nivy.base.model.TransactionType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * The net worth screen's [MonthDetail] of a period, from the accounts in the balance, amounts in
 * the base currency. Its balance day by day is the wallet's, as the screen's current balance is,
 * and ahead it adds what's due as the balance after planned payments does.
 */
class MonthDetailUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val walletBalanceUseCase: WalletBalanceUseCase,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
) {
    @Suppress("LongMethod")
    suspend fun detail(period: TimePeriod, startDayOfMonth: Int, baseCurrency: String): MonthDetail =
        withContext(dispatchers.io) {
            val rates = Rates(AssetCode.unsafe(baseCurrency))
            val range = period.toRange(startDayOfMonth, timeConverter, timeProvider)
            val now = timeProvider.utcNow()
            val days = days(range)
            val today = timeProvider.localDateNow().takeIf { it in days }
            val elapsed = when {
                today != null -> days.takeWhile { !it.isAfter(today) }
                range.to().isBefore(now) -> days
                else -> emptyList()
            }
            val accounts = accountRepository.findAll()
            val inBalance = accounts.filter { it.includeInBalance }
            val names = Names(accounts.associateBy { it.id }, categoryRepository.findAll().associateBy { it.id })

            val paid = paid(range.from(), minOf(range.to(), now), inBalance)
            val expenses = paid.filterIsInstance<Expense>()
            val spends = expenses.filter { it.isPurchase() }.map { spend(it, rates, names) }
            val income = paid.filterIsInstance<Income>().sumOf { rates.inBase(it.value) }
            val outgoing = expenses.sumOf { rates.inBase(it.value) }

            // What's due from the current month's start, overdue too, as the balance after planned
            // payments counts it.
            val currentStart = TimePeriod.currentMonth(startDayOfMonth, timeProvider)
                .toRange(startDayOfMonth, timeConverter, timeProvider).from()
            val due = if (range.to().isAfter(now)) {
                transactionRepository.findAllDueToBetween(currentStart, range.to()).filter { !it.settled }
            } else {
                emptyList()
            }
            val signedDue = due.map { it.time to rates.signed(it) }
            val balances = days.mapIndexed { index, day ->
                val end = endOf(day)
                if (index < elapsed.size) {
                    wallet(baseCurrency, minOf(end, now))
                } else {
                    wallet(baseCurrency, end) + signedDue.filter { !it.first.isAfter(end) }.sumOf { it.second }
                }
            }

            val startOfToday = today?.let { with(timeConverter) { it.atStartOfDay().toUTC() } }
            val dueInPeriod = due.filter { range.includes(it.time) }
            val plannedExpenses = dueInPeriod.filterIsInstance<Expense>().map { rates.inBase(it.value) }
            val ahead = due.filter { startOfToday == null || !it.time.isBefore(startOfToday) }
                .sortedBy { it.time }
            val previous = previousMonth(period, startDayOfMonth, inBalance, rates, names)
            val holdings = holdings(inBalance, rates)

            MonthDetail(
                days = days.toImmutableList(),
                today = today,
                balances = balances.toImmutableList(),
                actualDays = elapsed.size,
                sinceStart = if (today != null && elapsed.isNotEmpty()) {
                    balances[elapsed.size - 1] - wallet(baseCurrency, range.from().minusMillis(1))
                } else {
                    null
                },
                lowest = if (today != null) lowestAhead(days, balances, elapsed.size) else null,
                freeToSpend = if (today != null) {
                    freeToSpend(
                        income = income,
                        plannedIncome = dueInPeriod.filterIsInstance<Income>().sumOf { rates.inBase(it.value) },
                        spent = outgoing,
                        plannedExpenses = plannedExpenses.sum(),
                        daysLeft = days.size - elapsed.size,
                    )
                } else {
                    null
                },
                comingUp = ahead.take(ComingUpListed).map { upcoming(it, rates, names) }.toImmutableList(),
                moreComingUp = (ahead.size - ComingUpListed).coerceAtLeast(0),
                spentByDay = days.map { day -> spends.filter { it.day == day }.sumOf { it.amount } }.toImmutableList(),
                plannedDays = due.filterIsInstance<Expense>()
                    .map { it.time.localDate() }
                    .filter { day -> today != null && day.isAfter(today) && day in days }
                    .toImmutableSet(),
                pace = previous?.let { (month, previousSpends, previousDays) ->
                    Pace(
                        current = elapsed.map { day -> spends.filter { it.day == day }.sumOf { it.amount } }
                            .runningReduceOrEmpty().toImmutableList(),
                        previous = previousDays.map { day -> previousSpends.filter { it.day == day }.sumOf { it.amount } }
                            .runningReduceOrEmpty().toImmutableList(),
                        previousMonth = month.month,
                        comparedTo = previousDays.getOrNull(elapsed.size - 1),
                    )
                }?.takeIf { it.current.isNotEmpty() },
                habits = habits(
                    spends = spends,
                    elapsed = elapsed,
                    daysInMonth = days.size,
                    income = income,
                    outgoing = outgoing,
                    plannedExpenses = plannedExpenses,
                    previous = previous?.let { (_, previousSpends, previousDays) ->
                        val sameDays = previousDays.take(elapsed.size).toSet()
                        previousSpends.filter { it.day in sameDays }
                            .groupBy { it.category }
                            .mapValues { (_, inCategory) -> inCategory.sumOf { it.amount } }
                    }.orEmpty(),
                    edges = sizeEdges(rates.factor(Euro) ?: 1.0),
                ),
                holdings = holdings,
                runway = runway(holdings.netWorth, monthlySpending(startDayOfMonth, inBalance, rates)),
            )
        }

    private suspend fun wallet(baseCurrency: String, at: Instant): Double =
        walletBalanceUseCase.balance(baseCurrency, range = ClosedTimeRange.to(at)).toDouble()

    // The paid transactions from [from] to [to] of the [accounts].
    private suspend fun paid(from: Instant, to: Instant, accounts: List<Account>): List<Transaction> {
        if (to.isBefore(from)) return emptyList()
        val ids = accounts.map { it.id }.toSet()
        return transactionRepository.findAllBetween(from, to).filter { it.settled && it.accountId() in ids }
    }

    // The month before [period]: itself, its purchases and its days.
    private suspend fun previousMonth(
        period: TimePeriod,
        startDayOfMonth: Int,
        accounts: List<Account>,
        rates: Rates,
        names: Names,
    ): Triple<TimePeriod, List<Spend>, List<LocalDate>>? {
        val month = period.month ?: return null
        val before = month.incrementMonthPeriod(-1L, period.year ?: timeProvider.localDateNow().year)
        val range = before.toRange(startDayOfMonth, timeConverter, timeProvider)
        val spends = paid(range.from(), range.to(), accounts).filterIsInstance<Expense>()
            .filter { it.isPurchase() }
            .map { spend(it, rates, names) }
        return Triple(before, spends, days(range))
    }

    // What went out in each of the last full months before the current one.
    private suspend fun monthlySpending(startDayOfMonth: Int, accounts: List<Account>, rates: Rates): List<Double> {
        val current = TimePeriod.currentMonth(startDayOfMonth, timeProvider)
        val month = current.month ?: return emptyList()
        val year = current.year ?: timeProvider.localDateNow().year
        return (1..RunwayMonths).map { back ->
            val range = month.incrementMonthPeriod(-back.toLong(), year).toRange(startDayOfMonth, timeConverter, timeProvider)
            paid(range.from(), range.to(), accounts).filterIsInstance<Expense>().sumOf { rates.inBase(it.value) }
        }
    }

    private suspend fun holdings(accounts: List<Account>, rates: Rates): Holdings {
        val held = accounts.map { account ->
            Holding(account, rates.inBase(accountBalanceUseCase.balance(account).balance.toDouble(), account.asset))
        }
        return Holdings(
            assets = held.filter { it.amount > 0 }.sortedByDescending { it.amount }.toImmutableList(),
            debts = held.filter { it.amount < 0 }.sortedBy { it.amount }.toImmutableList(),
            netWorth = held.sumOf { it.amount },
        )
    }

    private suspend fun spend(expense: Expense, rates: Rates, names: Names) = Spend(
        amount = rates.inBase(expense.value),
        day = expense.time.localDate(),
        made = with(timeConverter) { (expense.metadata.purchaseDateTime ?: expense.time).toLocalDateTime() },
        title = expense.title?.value,
        category = expense.category?.let(names.categories::get),
        account = names.accounts[expense.account],
        recurring = expense.metadata.recurringRuleId != null,
    )

    private suspend fun upcoming(transaction: Transaction, rates: Rates, names: Names) = Upcoming(
        day = transaction.time.localDate(),
        title = transaction.title?.value,
        category = transaction.category?.let(names.categories::get),
        account = names.accounts[transaction.accountId()],
        type = when (transaction) {
            is Income -> TransactionType.INCOME
            is Expense -> TransactionType.EXPENSE
            is Transfer -> TransactionType.TRANSFER
        },
        amount = when (transaction) {
            is Transfer -> rates.inBase(transaction.fromValue)
            else -> rates.signed(transaction)
        },
    )

    private fun days(range: FromToTimeRange): List<LocalDate> {
        val first = range.from().localDate()
        val last = range.to().localDate()
        return generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
    }

    private fun endOf(day: LocalDate): Instant = with(timeConverter) { day.atTime(LocalTime.MAX).toUTC() }

    private fun Instant.localDate(): LocalDate = with(timeConverter) { toLocalDate() }

    private class Names(val accounts: Map<AccountId, Account>, val categories: Map<CategoryId, Category>)

    // A currency's worth in the base currency, read once for each currency.
    private inner class Rates(private val base: AssetCode) {
        private val factors = mutableMapOf<AssetCode, Double?>()

        suspend fun factor(currency: AssetCode): Double? = factors.getOrPut(currency) {
            if (currency == base) {
                1.0
            } else {
                exchangeUseCase.convert(PositiveValue(PositiveDouble.unsafe(1.0), currency), to = base)
                    .getOrNull()?.amount?.value
            }
        }

        // Without a rate, an amount is left out, as in every other total.
        suspend fun inBase(value: PositiveValue): Double = inBase(value.amount.value, value.asset)

        suspend fun inBase(amount: Double, currency: AssetCode): Double = amount * (factor(currency) ?: 0.0)

        // Income up, expenses down; a transfer stays within the wallet.
        suspend fun signed(transaction: Transaction): Double = when (transaction) {
            is Income -> inBase(transaction.value)
            is Expense -> -inBase(transaction.value)
            is Transfer -> 0.0
        }
    }

    private companion object {
        val Euro = AssetCode.unsafe("EUR")
        const val ComingUpListed = 5
        const val RunwayMonths = 3
    }
}

// A loan's money isn't a purchase.
private fun Expense.isPurchase(): Boolean = metadata.loanId == null && metadata.loanRecordId == null

private fun Transaction.accountId(): AccountId = when (this) {
    is Income -> account
    is Expense -> account
    is Transfer -> fromAccount
}

private fun List<Double>.runningReduceOrEmpty(): List<Double> = if (isEmpty()) this else runningReduce(Double::plus)
