package com.nivy.balance.usecase

import com.nivy.balance.detail.BudgetMonth
import com.nivy.balance.detail.FreeToSpend
import com.nivy.balance.detail.Holding
import com.nivy.balance.detail.Holdings
import com.nivy.balance.detail.MonthDetail
import com.nivy.balance.detail.MonthOutlook
import com.nivy.balance.detail.Pace
import com.nivy.balance.detail.PastMonth
import com.nivy.balance.detail.Unusual
import com.nivy.balance.detail.Upcoming
import com.nivy.balance.detail.freeToSpend
import com.nivy.balance.detail.habits
import com.nivy.balance.detail.lowestAhead
import com.nivy.balance.detail.monthOutlook
import com.nivy.balance.detail.runway
import com.nivy.balance.detail.unusual
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
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.expensesIn
import com.nivy.data.model.getFromAccount
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.spending.toFlow
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.spending.Flow
import com.nivy.wallet.domain.pure.spending.chosen
import com.nivy.wallet.domain.pure.spending.fixed
import com.nivy.wallet.domain.pure.util.roundNicely
import com.nivy.wallet.domain.pure.util.sizeEdges
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import kotlin.random.Random

/**
 * The net worth screen's [MonthDetail] of a period, from the accounts in the balance, amounts in
 * the base currency. Its balance day by day is the wallet's, as the screen's current balance is,
 * and ahead it adds what's due as the balance after planned payments does.
 */
class MonthDetailUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val walletBalanceUseCase: WalletBalanceUseCase,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun detail(period: TimePeriod, startDayOfMonth: Int, baseCurrency: String): MonthDetail =
        withContext(dispatchers.io) {
            val rates = BaseCurrencyRates(exchangeUseCase, AssetCode.unsafe(baseCurrency))
            val range = period.toRange(startDayOfMonth, timeConverter, timeProvider)
            val now = timeProvider.utcNow()
            val days = range.days(timeConverter)
            val today = timeProvider.localDateNow().takeIf { it in days }
            val elapsed = when {
                today != null -> days.takeWhile { !it.isAfter(today) }
                range.to().isBefore(now) -> days
                else -> emptyList()
            }
            val accounts = accountRepository.findAll()
            val inBalance = accounts.filter { it.includeInBalance }
            val upToNow = minOf(range.to(), now)
            val month = Month(
                period = period,
                startDayOfMonth = startDayOfMonth,
                days = days,
                today = today,
                elapsed = elapsed,
                first = transactionRepository.findFirstPaidTime()?.localDate(),
                rates = rates,
                names = Names(accounts.associateBy { it.id }, categoryRepository.findAll().associateBy { it.id }),
                // What the detail looks back on, read once: the months before the period, to its end.
                paid = PaidTransactions(inBalance, lookBackFrom(period, startDayOfMonth, range), range.to()),
            )
            val paid = month.paid.between(range.from(), upToNow)
            val spends = month.purchases(paid)
            val income = paid.filterIsInstance<Income>().sumOf { rates.inBase(it.value) }
            val outgoing = paid.filterIsInstance<Expense>().sumOf { rates.inBase(it.value) }

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
            val ahead = due.filter { startOfToday == null || !it.time.isBefore(startOfToday) }
                .sortedBy { it.time }
            val previous = month.previous()
            val holdings = holdings(inBalance, rates)
            // The budgets count the month's expenses as Budgets does, of every account. Free to spend
            // spends from the total one, the first that counts every category.
            val budgets = budgetRepository.findAll()
            val inMonth = month.paid.ofEveryAccount(range.from(), range.to())
            val total = budgets.firstOrNull { it.categories.isEmpty() }
            val daysLeft = days.size - elapsed.size
            val free = if (today != null && total != null) {
                freeToSpend(
                    budget = total.amount.value,
                    spent = total.expensesIn(inMonth).sumOf { rates.inBase(it.value) },
                    plannedExpenses = total.expensesIn(dueInPeriod).sumOf { rates.inBase(it.value) },
                    daysLeft = daysLeft,
                )
            } else {
                null
            }
            val outlook = month.outlook(balances, spends, due, free)
            val unusual = month.unusualSoFar(spends)

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
                freeToSpend = free,
                needsBudget = today != null && total == null && daysLeft > 0,
                budgets = budgets.map { budget ->
                    BudgetMonth(
                        name = budget.name.value,
                        limit = budget.amount.value,
                        spent = budget.expensesIn(inMonth).sumOf { rates.inBase(it.value) },
                    )
                }.toImmutableList(),
                comingUp = ahead.take(ComingUpListed).map { month.upcoming(it) }.toImmutableList(),
                moreComingUp = (ahead.size - ComingUpListed).coerceAtLeast(0),
                spentByDay = spentOn(days, spends).toImmutableList(),
                plannedDays = due.filterIsInstance<Expense>()
                    .map { it.time.localDate() }
                    .filter { day -> today != null && day.isAfter(today) && day in days }
                    .toImmutableSet(),
                pace = previous?.let { (month, previousSpends, previousDays) ->
                    Pace(
                        current = spentOn(elapsed, spends).runningReduceOrEmpty().toImmutableList(),
                        previous = spentOn(previousDays, previousSpends).runningReduceOrEmpty().toImmutableList(),
                        previousMonth = month.month,
                        comparedTo = previousDays.getOrNull(elapsed.size - 1),
                    )
                }?.takeIf { it.current.isNotEmpty() },
                habits = habits(
                    spends = spends,
                    elapsed = elapsed,
                    trackedSince = month.first,
                    income = income,
                    outgoing = outgoing,
                    plannedFixed = month.purchases(dueInPeriod).fixed().map { it.amount },
                    previous = previous?.let { (_, previousSpends, previousDays) ->
                        val sameDays = previousDays.take(elapsed.size).toSet()
                        previousSpends.filter { it.day in sameDays }
                            .groupBy { it.category }
                            .mapValues { (_, inCategory) -> inCategory.sumOf { it.amount } }
                    }.orEmpty(),
                    edges = sizeEdges(rates.factor(AssetCode.EUR) ?: 1.0),
                ),
                holdings = holdings,
                runway = runway(holdings.netWorth, month.monthlySpending()),
                outlook = outlook,
                unusual = unusual,
            )
        }

    private suspend fun wallet(baseCurrency: String, at: Instant): Double =
        walletBalanceUseCase.balance(baseCurrency, range = ClosedTimeRange.to(at)).toDouble()

    /**
     * The paid transactions, read once from [from] to [to] and then sliced in memory; a range
     * reaching outside it is read on its own. [between] slices those of the [accounts] in the
     * balance, as the stats screens count them, and [ofEveryAccount] those of every account, as
     * Budgets does.
     */
    private inner class PaidTransactions(
        accounts: List<Account>,
        private val from: Instant,
        private val to: Instant,
    ) {
        private val ids = accounts.map { it.id }.toSet()
        private var held: List<Transaction>? = null

        suspend fun between(start: Instant, end: Instant): List<Transaction> =
            ofEveryAccount(start, end).filter { it.getFromAccount() in ids }

        suspend fun ofEveryAccount(start: Instant, end: Instant): List<Transaction> = when {
            end.isBefore(start) -> emptyList()
            start.isBefore(from) || end.isAfter(to) -> read(start, end)
            else -> (held ?: read(from, to).also { held = it }).filter { it.time in start..end }
        }

        private suspend fun read(start: Instant, end: Instant): List<Transaction> =
            transactionRepository.findAllBetween(start, end).filter { it.settled }
    }

    // Where the months before [period] that the detail looks back on begin.
    private fun lookBackFrom(period: TimePeriod, startDayOfMonth: Int, range: FromToTimeRange): Instant {
        val month = period.month ?: return range.from()
        val year = period.year ?: timeProvider.localDateNow().year
        return month.incrementMonthPeriod(-PastMonthsCounted.toLong(), year)
            .toRange(startDayOfMonth, timeConverter, timeProvider)
            .from()
    }

    // The month before [period]: itself, its purchases and its days.
    private suspend fun Month.previous(): Triple<TimePeriod, List<Flow>, List<LocalDate>>? {
        val month = period.month ?: return null
        val before = month.incrementMonthPeriod(-1L, period.year ?: timeProvider.localDateNow().year)
        val range = before.toRange(startDayOfMonth, timeConverter, timeProvider)
        return Triple(before, purchases(paid.between(range.from(), range.to())), range.days(timeConverter))
    }

    /**
     * While the month runs, to its today, what it likely holds still: from the everyday spending
     * since the first paid transaction's day, the [spends] so far and the purchases [due].
     */
    private suspend fun Month.outlook(
        balances: List<Double>,
        spends: List<Flow>,
        due: List<Transaction>,
        free: FreeToSpend?,
    ): MonthOutlook? {
        if (today == null || first == null) return null
        val planned = purchases(due)
        return monthOutlook(
            days = days,
            actualDays = elapsed.size,
            balances = balances,
            spentSoFar = spends.sumOf { it.amount },
            planned = (elapsed.size - 1 until days.size).map { index ->
                planned.filter { !it.day.isAfter(days[index]) }.sumOf { it.amount }
            },
            history = everydayHistory(today, first),
            free = free,
            // The same runs all day, so the figures hold still while the day's data does.
            random = Random(today.toEpochDay()),
        )
    }

    // While the month runs, to its today, what stands out in its [spends] so far against the months
    // since the first paid transaction's day.
    private suspend fun Month.unusualSoFar(spends: List<Flow>): Unusual? {
        if (today == null || first == null) return null
        return unusual(
            spends = spends,
            elapsed = elapsed.size,
            past = pastMonths(first),
            atLeast = roundNicely(UnusualEuros * (rates.factor(AssetCode.EUR) ?: 1.0)),
        )
    }

    /**
     * The everyday purchases' total on each day of the last weeks before [today], from the [first]
     * paid transaction's day on, days without any at nothing. A planned payment's bill, or a loan's
     * money, isn't everyday spending.
     */
    private suspend fun Month.everydayHistory(today: LocalDate, first: LocalDate): Map<LocalDate, Double> {
        val from = maxOf(today.minusDays(HistoryDays), first)
        val byDay = purchases(paid.between(startOf(from), startOf(today).minusMillis(1))).chosen()
            .groupBy { it.day }
            .mapValues { (_, inDay) -> inDay.sumOf { it.amount } }
        return generateSequence(from) { it.plusDays(1) }.takeWhile { it.isBefore(today) }
            .associateWith { byDay[it] ?: 0.0 }
    }

    // The months before [period], oldest first, those that began on or after the [first] paid
    // transaction's day: each one's days and purchases.
    private suspend fun Month.pastMonths(first: LocalDate): List<PastMonth> {
        val month = period.month ?: return emptyList()
        val year = period.year ?: timeProvider.localDateNow().year
        val ranges = (PastMonthsCounted downTo 1)
            .map { back ->
                month.incrementMonthPeriod(-back.toLong(), year).toRange(startDayOfMonth, timeConverter, timeProvider)
            }
            .filter { !it.from().localDate().isBefore(first) }
        if (ranges.isEmpty()) return emptyList()
        val spends = purchases(paid.between(ranges.first().from(), ranges.last().to()))
        return ranges.map { range ->
            val days = range.days(timeConverter)
            val inMonth = days.toSet()
            PastMonth(YearMonth.from(days.first()), days, spends.filter { it.day in inMonth })
        }
    }

    // What went out in each of the last full months before the current one.
    private suspend fun Month.monthlySpending(): List<Double> {
        val current = TimePeriod.currentMonth(startDayOfMonth, timeProvider)
        val month = current.month ?: return emptyList()
        val year = current.year ?: timeProvider.localDateNow().year
        return (1..RunwayMonths).map { back ->
            val range = month.incrementMonthPeriod(
                -back.toLong(),
                year
            ).toRange(startDayOfMonth, timeConverter, timeProvider)
            paid.between(range.from(), range.to()).filterIsInstance<Expense>().sumOf { rates.inBase(it.value) }
        }
    }

    private suspend fun holdings(accounts: List<Account>, rates: BaseCurrencyRates): Holdings {
        val held = accounts.map { account ->
            Holding(account, rates.inBase(accountBalanceUseCase.balance(account).balance.toDouble(), account.asset))
        }
        return Holdings(
            assets = held.filter { it.amount > 0 }.sortedByDescending { it.amount }.toImmutableList(),
            debts = held.filter { it.amount < 0 }.sortedBy { it.amount }.toImmutableList(),
            netWorth = held.sumOf { it.amount },
        )
    }

    private suspend fun Month.upcoming(transaction: Transaction) = Upcoming(
        day = transaction.time.localDate(),
        title = transaction.title?.value,
        category = transaction.category?.let(names.categories::get),
        account = names.accounts[transaction.getFromAccount()],
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

    private fun endOf(day: LocalDate): Instant = with(timeConverter) { day.atTime(LocalTime.MAX).toUTC() }

    private fun startOf(day: LocalDate): Instant = with(timeConverter) { day.atStartOfDay().toUTC() }

    private fun Instant.localDate(): LocalDate = with(timeConverter) { toLocalDate() }

    // The purchases among the [transactions], as flows in the base currency.
    private suspend fun Month.purchases(transactions: List<Transaction>): List<Flow> =
        transactions.mapNotNull { it.toFlow(rates, timeConverter, names.categories, names.accounts) }
            .filter { it.purchase }

    private class Names(val accounts: Map<AccountId, Account>, val categories: Map<CategoryId, Category>)

    /**
     * What a period's detail is made from, gathered once: its [days], [today] when it's one of
     * them and those [elapsed] so far; the [first] paid transaction's day, with which the history
     * the forecasts draw on starts, the days before it untracked; the rates and names its amounts
     * and rows take; and the [paid] transactions it looks back on.
     */
    private class Month(
        val period: TimePeriod,
        val startDayOfMonth: Int,
        val days: List<LocalDate>,
        val today: LocalDate?,
        val elapsed: List<LocalDate>,
        val first: LocalDate?,
        val rates: BaseCurrencyRates,
        val names: Names,
        val paid: PaidTransactions,
    )

    private companion object {
        const val ComingUpListed = 5
        const val RunwayMonths = 3

        // The outlook draws on the days of the last 13 weeks; unusual spending is set against the
        // last 12 months, by 10 euros or more.
        const val HistoryDays = 91L
        const val PastMonthsCounted = 12
        const val UnusualEuros = 10.0
    }
}

// What the [spends] came to on each of the [days].
private fun spentOn(days: List<LocalDate>, spends: List<Flow>): List<Double> =
    days.map { day -> spends.filter { it.day == day }.sumOf { it.amount } }

private fun List<Double>.runningReduceOrEmpty(): List<Double> = if (isEmpty()) this else runningReduce(Double::plus)
