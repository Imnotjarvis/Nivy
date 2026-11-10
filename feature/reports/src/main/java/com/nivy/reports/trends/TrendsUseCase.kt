package com.nivy.reports.trends

import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.util.roundNicely
import com.nivy.wallet.domain.pure.util.smallPurchaseEdge
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.random.Random

/**
 * The [TrendsDetail] of a range of months up to the current one. Income, expenses and habits are
 * the paid transactions of the accounts in the balance, as Home counts them; the net worth and the
 * accounts' balances are every account's at each month's end, or now for the current month, as
 * the Accounts tab counts them: investment accounts at their value.
 */
class TrendsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun trends(range: TrendsRange, startDayOfMonth: Int, baseCurrency: AssetCode): TrendsDetail =
        withContext(dispatchers.io) {
            val rates = BaseCurrencyRates(exchangeUseCase, baseCurrency)
            val now = timeProvider.utcNow()
            val today = timeProvider.localDateNow()
            val current = TimePeriod.currentMonth(startDayOfMonth, timeProvider)
            val firstPaid = transactionRepository.findFirstPaidTime()
            fun month(back: Int): Pair<MonthSpan, FromToTimeRange> {
                val period = requireNotNull(current.month)
                    .incrementMonthPeriod(-back.toLong(), current.year ?: today.year)
                val bounds = period.toRange(startDayOfMonth, timeConverter, timeProvider)
                return MonthSpan(period, days(bounds), complete = bounds.to().isBefore(now)) to bounds
            }

            val count = range.months ?: monthsSince(firstPaid, ::month)
            val months = (count - 1 downTo 0).map(::month)
            // The categories' change looks at the 6 complete months before the current one.
            val changeMonths = (ChangeMonthsBack downTo 1).map(::month)
            val from = minOf(months.first().second.from(), changeMonths.first().second.from())

            val accounts = accountRepository.findAll()
            val included = accounts.filter { it.includeInBalance }.map { it.id }.toSet()
            val categories = categoryRepository.findAll().associateBy { it.id }
            val flows = transactionRepository.findAllBetween(from, now)
                .filter { it.settled && it.accountId() in included }
                .mapNotNull { flow(it, rates, categories) }

            // Each account's balance at each month's end, or now for the current month: over the
            // range, and over the months before the current one the outlook draws on.
            val ends = (maxOf(count, OutlookMonths + 1) - 1 downTo 0).map(::month)
            val held = accounts.map { account ->
                account to ends.map { (_, bounds) ->
                    val at = ClosedTimeRange.to(minOf(bounds.to(), now))
                    rates.inBase(accountBalanceUseCase.balance(account, at).balance.toDouble(), account.asset)
                }
            }
            val balances = held.map { (account, values) -> account to values.takeLast(count) }
            val worth = ends.indices.map { index -> held.sumOf { (_, values) -> values[index] } }
            val trackedSince = firstPaid?.let { with(timeConverter) { it.toLocalDate() } } ?: today
            val perEuro = rates.factor(Euro) ?: 1.0

            val detail = trends(
                months = months.map { it.first },
                changeMonths = changeMonths.map { it.first },
                flows = flows,
                netWorth = worth.takeLast(count),
                balances = balances,
                today = today,
                trackedSince = trackedSince,
                smallEdge = smallPurchaseEdge(perEuro),
            )
            // How the net worth moved in each complete month that began once the app was in use.
            val changes = ends.indices.drop(1)
                .filter { index ->
                    val (span, _) = ends[index]
                    span.complete && !span.days.first().isBefore(trackedSince)
                }
                .map { index -> worth[index] - worth[index - 1] }
            detail.copy(
                // The same runs all day, so the figures hold still while the day's data does.
                outlook = netWorthOutlook(
                    start = worth.last(),
                    changes = changes.takeLast(OutlookMonths),
                    now = YearMonth.of(current.year ?: today.year, requireNotNull(current.month).monthValue),
                    random = Random(today.toEpochDay()),
                ),
                shifts = habitShifts(detail.months, months.map { it.first }, flows, roundNicely(ShiftEuros * perEuro))
                    .toImmutableList(),
            )
        }

    // How many months back the first paid transaction is, the current month counted; at most
    // [AllMonthsAtMost].
    private fun monthsSince(first: Instant?, month: (Int) -> Pair<MonthSpan, FromToTimeRange>): Int {
        if (first == null) return 1
        return (1..AllMonthsAtMost).firstOrNull { count -> !month(count - 1).second.from().isAfter(first) }
            ?: AllMonthsAtMost
    }

    // An amount without a rate to the base currency is left out, not counted as one of nothing.
    private suspend fun flow(
        transaction: Transaction,
        rates: BaseCurrencyRates,
        categories: Map<CategoryId, Category>,
    ) =
        when (transaction) {
            is Income -> rates.inBaseOrNull(transaction.value)
                ?.let { transaction.toFlow(it, income = true, categories) }
            is Expense -> rates.inBaseOrNull(transaction.value)
                ?.let { transaction.toFlow(it, income = false, categories) }
            is Transfer -> null
        }

    private fun Transaction.toFlow(amount: Double, income: Boolean, categories: Map<CategoryId, Category>) = Flow(
        amount = amount,
        income = income,
        day = with(timeConverter) { time.toLocalDate() },
        made = with(timeConverter) { (metadata.purchaseDateTime ?: time).toLocalDateTime() },
        title = title?.value,
        category = category?.let(categories::get),
        recurring = metadata.recurringRuleId != null,
        // A loan's money isn't a purchase.
        purchase = !income && metadata.loanId == null && metadata.loanRecordId == null,
    )

    private fun days(range: FromToTimeRange): List<LocalDate> = with(timeConverter) {
        val last = range.to().toLocalDate()
        generateSequence(range.from().toLocalDate()) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
    }

    private companion object {
        val Euro = AssetCode.unsafe("EUR")
        const val ChangeMonthsBack = 6

        // A habit shift moves a category by 10 euros a month or more.
        const val ShiftEuros = 10.0
    }
}

private fun Transaction.accountId(): AccountId = when (this) {
    is Income -> account
    is Expense -> account
    is Transfer -> fromAccount
}
