package com.nivy.balance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.nivy.balance.detail.FreeToSpend
import com.nivy.balance.detail.Holding
import com.nivy.balance.detail.Holdings
import com.nivy.balance.detail.MonthDetail
import com.nivy.balance.detail.Pace
import com.nivy.balance.detail.SizeEdgesInEuros
import com.nivy.balance.detail.Spend
import com.nivy.balance.detail.Upcoming
import com.nivy.balance.detail.habits
import com.nivy.balance.detail.lowestAhead
import com.nivy.balance.detail.runway
import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * The net worth screen on November 9th, 2026, a Monday, for its screenshots: nine days of a
 * month's purchases and the planned payments still to come, figured as the screen figures them.
 */
internal object BalanceSample {
    /** November 9, 2026, noon, in UTC. */
    val November9 = object : TimeProvider {
        private val now = LocalDateTime.of(2026, 11, 9, 12, 0)
        override fun getZoneId(): ZoneId = ZoneId.of("UTC")
        override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
        override fun localNow(): LocalDateTime = now
        override fun localDateNow(): LocalDate = now.toLocalDate()
        override fun localTimeNow(): LocalTime = now.toLocalTime()
    }

    private val Groceries = category("Groceries", Palette.Green2, "groceries", 1)
    private val EatingOut = category("Eating out", Palette.Orange, "restaurant", 2)
    private val Transport = category("Transport", Palette.Blue, "transport", 3)
    private val Shopping = category("Shopping", Palette.Purple1, "shopping", 4)
    private val Subscriptions = category("Subscriptions", Palette.Red, "music", 5)
    private val Leisure = category("Leisure", Palette.Yellow, "game", 6)
    private val Housing = category("Housing", Palette.Blue3, "house", 7)
    private val Bills = category("Bills", Palette.Orange2, "bills", 8)

    private val MainBank = account("Main Bank", Palette.Blue3, "bank", 1)
    private val Savings = account("Savings", Palette.Green, "safe", 2, AccountType.SAVINGS)
    private val Cash = account("Cash", Palette.Orange3, "cash", 3, AccountType.CASH)
    private val CreditCard = account("Credit Card", Palette.Purple1, null, 4, AccountType.CREDIT_CARD)

    private val Days = (1..30).map { LocalDate.of(2026, 11, it) }
    private val Today = Days[8]
    private const val Income = 1_650.0
    private const val StartBalance = 6_812.40

    // November's purchases by the 9th.
    private val Purchases = listOf(
        spend(1, 9, "Bar Roma", EatingOut, 3.80),
        spend(1, 18, "Lidl", Groceries, 18.50),
        spend(2, 7, "Spotify", Subscriptions, 10.99, recurring = true),
        spend(2, 8, "Bar Roma", EatingOut, 2.50),
        spend(2, 23, "Uber", Transport, 18.81, account = CreditCard),
        spend(4, 6, "Netflix", Subscriptions, 13.99, recurring = true, account = CreditCard),
        spend(4, 9, "Bar Roma", EatingOut, 3.20, account = Cash),
        spend(4, 12, "Bakery", Groceries, 6.36, account = Cash),
        spend(5, 17, "Home goods", Shopping, 130.30, account = CreditCard),
        spend(7, 11, "Lidl", Groceries, 24.60),
        spend(7, 20, "Trattoria", EatingOut, 58.40, account = CreditCard),
        spend(7, 22, "Bar Roma", EatingOut, 7.00, account = Cash),
        spend(8, 1, "Uber", Transport, 12.90, account = CreditCard),
        spend(8, 10, "Bakery", Groceries, 5.10, account = Cash),
        spend(8, 21, "Cinema", Leisure, 24.00),
        spend(9, 8, "Bar Roma", EatingOut, 2.80, account = Cash),
        spend(9, 19, "Lidl", Groceries, 9.60),
    )

    private val ComingUp = listOf(
        Upcoming(Days[9], "Gym", Leisure, MainBank, TransactionType.EXPENSE, -35.0),
        Upcoming(Days[13], "Car insurance", Bills, MainBank, TransactionType.EXPENSE, -120.55),
        Upcoming(Days[17], "Electricity", Bills, MainBank, TransactionType.EXPENSE, -40.0),
        Upcoming(Days[19], "Refund", Shopping, CreditCard, TransactionType.INCOME, 60.0),
        Upcoming(Days[24], "Rent", Housing, MainBank, TransactionType.EXPENSE, -550.0),
        Upcoming(Days[27], "Cloud storage", Subscriptions, CreditCard, TransactionType.EXPENSE, -2.99),
    )

    // What October's purchases came to on each of its days.
    private val October = listOf(
        42.10, 18.40, 0.0, 61.25, 12.80, 96.40, 25.10, 0.0, 154.15, 33.60, 8.20, 0.0, 47.90, 22.35, 11.80, 74.60,
        0.0, 29.40, 15.75, 0.0, 38.20, 66.10, 9.90, 0.0, 120.45, 14.30, 0.0, 27.80, 31.25, 18.60, 45.95,
    )

    private val Detail: MonthDetail = run {
        val spentByDay = Days.map { day -> Purchases.filter { it.day == day }.sumOf { it.amount } }
        val balances = Days.indices.runningFold(StartBalance) { balance, index ->
            val day = Days[index]
            val paid = if (day.isAfter(Today)) 0.0 else -spentByDay[index] + if (index == 1) Income else 0.0
            balance + paid + ComingUp.filter { it.day == day }.sumOf { it.amount }
        }.drop(1)
        val spent = Purchases.sumOf { it.amount }
        val plannedExpenses = ComingUp.filter { it.type == TransactionType.EXPENSE }.map { -it.amount }
        val left = Income + 60.0 - spent - plannedExpenses.sum()
        val elapsed = Days.take(9)
        val holdings = listOf(
            Holding(MainBank, 6_240.30),
            Holding(Savings, 2_500.0),
            Holding(Cash, 120.25),
            Holding(CreditCard, -751.0),
        )
        MonthDetail(
            days = Days.toImmutableList(),
            today = Today,
            balances = balances.toImmutableList(),
            actualDays = elapsed.size,
            sinceStart = balances[8] - StartBalance,
            lowest = lowestAhead(Days, balances, elapsed.size),
            freeToSpend = FreeToSpend(perDay = left / 21, daysLeft = 21, spent = spent, left = left),
            comingUp = ComingUp.take(5).toImmutableList(),
            moreComingUp = ComingUp.size - 5,
            spentByDay = spentByDay.toImmutableList(),
            plannedDays = ComingUp.filter { it.type == TransactionType.EXPENSE }.map { it.day }.toImmutableSet(),
            pace = Pace(
                current = spentByDay.take(9).runningReduce(Double::plus).toImmutableList(),
                previous = October.runningReduce(Double::plus).toImmutableList(),
                previousMonth = Month.fromMonthValue(10),
                comparedTo = LocalDate.of(2026, 10, 9),
            ),
            habits = habits(
                spends = Purchases,
                elapsed = elapsed,
                daysInMonth = Days.size,
                income = Income,
                outgoing = spent,
                plannedExpenses = plannedExpenses,
                previous = mapOf(
                    Groceries to 72.90,
                    EatingOut to 34.80,
                    Transport to 46.20,
                    Subscriptions to 24.98,
                    Leisure to 24.0,
                ),
                edges = SizeEdgesInEuros,
            ),
            holdings = Holdings(
                assets = holdings.filter { it.amount > 0 }.toImmutableList(),
                debts = holdings.filter { it.amount < 0 }.toImmutableList(),
                netWorth = holdings.sumOf { it.amount },
            ),
            runway = runway(netWorth = holdings.sumOf { it.amount }, monthlySpending = listOf(1_240.0, 1_105.60, 1_312.80)),
        )
    }

    val State = BalanceState(
        period = TimePeriod(month = Month.fromMonthValue(11), year = 2026),
        baseCurrencyCode = "EUR",
        currentBalance = Detail.balances[8],
        plannedPaymentsAmount = ComingUp.sumOf { it.amount },
        balanceAfterPlannedPayments = Detail.balances.last(),
        detail = Detail,
    )

    @Suppress("LongParameterList")
    private fun spend(
        day: Int,
        hour: Int,
        title: String,
        category: Category,
        amount: Double,
        recurring: Boolean = false,
        account: Account = MainBank,
    ) = Spend(
        amount = amount,
        day = LocalDate.of(2026, 11, day),
        made = LocalDateTime.of(2026, 11, day, hour, 0),
        title = title,
        category = category,
        account = account,
        recurring = recurring,
    )

    private fun category(name: String, color: Color, icon: String, n: Long) = Category(
        id = CategoryId(UUID(9L, n)),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(color.toArgb()),
        icon = IconAsset.unsafe(icon),
        orderNum = n.toDouble(),
    )

    private fun account(name: String, color: Color, icon: String?, n: Long, type: AccountType = AccountType.BANK) =
        Account(
            id = AccountId(UUID(1L, n)),
            name = NotBlankTrimmedString.unsafe(name),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(color.toArgb()),
            icon = icon?.let(IconAsset::unsafe),
            includeInBalance = true,
            type = type,
            archived = false,
            orderNum = n.toDouble(),
        )
}
