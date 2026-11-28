package com.nivy.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.entity.PaymentNotificationEntity
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Transaction
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.domain.notification.ListedNotification
import com.nivy.domain.notification.Outcome
import com.nivy.domain.notification.Stopped
import com.nivy.domain.notification.Totals
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/** Home with the payments added from notifications, and with its warning when Nivy stopped reading them. */
@RunWith(TestParameterInjector::class)
class HomeNotificationsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    // The newest three added, the third's card account to check, then See all and All good.
    @Test
    fun `snapshot Home with the payments added from notifications`() = shot { it.copy(notifications = Notifications) }

    // A day without hearing any notification.
    @Test
    fun `snapshot Home's warning when Nivy stopped reading notifications`() = shot {
        it.copy(listeningStopped = Stopped(accessOff = false, since = at(27, 9, 12)))
    }

    private fun shot(change: (HomeState) -> HomeState) = snapshot(theme, minimal = colors == Colors.Minimal) {
        HomeUiTest(
            isDark = theme == PaparazziTheme.Dark,
            stats = IncomeExpensePair(income = BigDecimal("1650"), expense = BigDecimal("352.85")),
            timeProvider = FixedTime,
        ) { state ->
            change(
                state.copy(baseData = BaseData, history = History, balance = BigDecimal("8109.55"), period = November)
            )
        }
    }
}

/** Saturday November 28, 2026, at 14:30 in UTC. */
internal object FixedTime : TimeProvider {
    private val now = LocalDateTime.of(2026, 11, 28, 14, 30)
    override fun getZoneId(): ZoneId = ZoneId.of("UTC")
    override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
    override fun localNow(): LocalDateTime = now
    override fun localDateNow(): LocalDate = now.toLocalDate()
    override fun localTimeNow(): LocalTime = now.toLocalTime()
}

internal val November = TimePeriod(month = Month.fromMonthValue(11), year = 2026)

internal fun at(day: Int, hour: Int, minute: Int): Instant =
    LocalDateTime.of(2026, 11, day, hour, minute).toInstant(ZoneOffset.UTC)

private fun id(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())

private fun category(name: String, color: Color, icon: String) = Category(
    id = CategoryId(id(name)),
    name = NotBlankTrimmedString.unsafe(name),
    color = ColorInt(color.toArgb()),
    icon = IconAsset.unsafe(icon),
    orderNum = 0.0,
)

private val Intesa = previewAccount("Intesa", Palette.Blue, currency = "EUR", id = id("Intesa"))
private val Revolut = previewAccount("Revolut", Palette.Purple1, currency = "EUR", id = id("Revolut"))
private val Groceries = category("Groceries", Palette.Green2, "groceries")
private val EatingOut = category("Eating out", Palette.Orange, "restaurant")
private val Transport = category("Transport", Palette.Blue, "transport")
internal val BaseData = AppBaseData(
    baseCurrency = "EUR",
    accounts = persistentListOf(Intesa, Revolut),
    categories = persistentListOf(Groceries, EatingOut, Transport),
)

private fun spent(time: Instant, title: String, category: Category, amount: Double, account: Account) =
    previewTransaction(
        type = TransactionType.EXPENSE,
        account = account,
        amount = amount,
        time = time,
        title = title,
        category = category.id,
        id = id(title),
    )

private val Conad = spent(at(28, 12, 4), "CONAD SUPERSTORE", Groceries, 12.50, Intesa)
private val Vega = spent(at(28, 8, 31), "Vega Carburanti", Transport, 10.0, Revolut)
private val BarRoma = spent(at(27, 19, 42), "Bar Roma", EatingOut, 4.20, Intesa)

internal val History = persistentListOf(
    TransactionHistoryDateDivider(LocalDate.of(2026, 11, 28), income = 0.0, expenses = 22.50),
    TransactionHistoryEntry(Conad),
    TransactionHistoryEntry(Vega),
    TransactionHistoryDateDivider(LocalDate.of(2026, 11, 27), income = 0.0, expenses = 42.60),
    TransactionHistoryEntry(spent(at(27, 20, 15), "Trattoria", EatingOut, 38.40, Intesa)),
    TransactionHistoryEntry(BarRoma),
)

private fun added(transaction: Transaction, app: String, card: String? = null) = ListedNotification(
    notification = PaymentNotificationEntity(
        id = id("$app ${transaction.time}"),
        packageName = "app.$app",
        appName = app,
        notificationKey = "$app ${transaction.time}",
        postTime = transaction.time,
        title = transaction.title?.value,
        text = "",
        outcome = Outcome.Added.name,
        why = null,
        way = "Spent",
        cents = null,
        currency = null,
        merchant = transaction.title?.value,
        card = card,
        transactionId = transaction.id.value,
        about = null,
        aboutTime = null,
        guessedAccount = card != null,
        reviewed = false,
    ),
    outcome = Outcome.Added,
    why = null,
    transaction = transaction,
    alsoIn = emptyList(),
    since = null,
)

internal val Notifications = HomeNotifications(
    latest = persistentListOf(
        added(Conad, "Intesa Sanpaolo"),
        added(Vega, "Revolut"),
        added(BarRoma, "Google Wallet", card = "1501"),
    ),
    count = 8,
    notAdded = 3,
    totals = Totals(income = 0.0, expenses = 50.69),
    expanded = true,
)
