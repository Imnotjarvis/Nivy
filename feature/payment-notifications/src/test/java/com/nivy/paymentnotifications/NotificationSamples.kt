package com.nivy.paymentnotifications

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.nivy.domain.notification.AlsoIn
import com.nivy.domain.notification.ListedNotification
import com.nivy.domain.notification.MoneyWay
import com.nivy.domain.notification.Outcome
import com.nivy.domain.notification.PaymentNotificationReader
import com.nivy.domain.notification.Why
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/** Saturday November 28, 2026, at 14:30 in UTC: the screenshots' today. */
internal object FixedTime : TimeProvider {
    private val now = LocalDateTime.of(2026, 11, 28, 14, 30)
    override fun getZoneId(): ZoneId = ZoneId.of("UTC")
    override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
    override fun localNow(): LocalDateTime = now
    override fun localDateNow(): LocalDate = now.toLocalDate()
    override fun localTimeNow(): LocalTime = now.toLocalTime()
}

internal const val IntesaApp = "com.latuabancaperandroid"
internal const val RevolutApp = "com.revolut.revolut"
internal const val Wallet = PaymentNotificationReader.GoogleWallet

internal val Intesa = previewAccount("Intesa", Palette.Blue, currency = "EUR", id = id("Intesa"))
internal val Revolut = previewAccount("Revolut", Palette.Purple1, currency = "EUR", id = id("Revolut"))
internal val Cash = previewAccount("Cash", Palette.Green2, currency = "EUR", id = id("Cash"))
internal val Groceries = category("Groceries", Palette.Green2, "groceries")
internal val EatingOut = category("Eating out", Palette.Orange, "restaurant")
internal val Transport = category("Transport", Palette.Blue, "transport")
internal val Shopping = category("Shopping", Palette.Purple1, "shopping")
internal val SampleBaseData = AppBaseData(
    baseCurrency = "EUR",
    accounts = persistentListOf(Intesa, Revolut, Cash),
    categories = persistentListOf(Groceries, EatingOut, Transport, Shopping),
)

/** [hour]:[minute] on November [day], 2026, in UTC. */
internal fun at(day: Int, hour: Int, minute: Int): Instant =
    LocalDateTime.of(2026, 11, day, hour, minute).toInstant(ZoneOffset.UTC)

/** A notification Nivy read, and what became of it. */
@Suppress("LongParameterList")
internal fun read(
    packageName: String,
    app: String,
    time: Instant,
    title: String?,
    text: String,
    outcome: Outcome,
    why: Why? = null,
    way: MoneyWay? = MoneyWay.Spent,
    cents: Long? = null,
    merchant: String? = null,
    card: String? = null,
    transaction: Transaction? = null,
    guessed: Boolean = false,
) = PaymentNotificationEntity(
    id = id("$app $time"),
    packageName = packageName,
    appName = app,
    notificationKey = "$app $time",
    postTime = time,
    title = title,
    text = text,
    outcome = outcome.name,
    why = why?.name,
    way = way?.name,
    cents = cents,
    currency = cents?.let { "EUR" },
    merchant = merchant,
    card = card,
    transactionId = transaction?.id?.value,
    about = null,
    aboutTime = null,
    guessedAccount = guessed,
    reviewed = false,
)

/** An expense added from a notification. */
internal fun added(time: Instant, title: String?, category: Category?, amount: Double, account: Account = Intesa) =
    previewTransaction(
        type = TransactionType.EXPENSE,
        account = account,
        amount = amount,
        time = time,
        title = title,
        category = category?.id,
    )

internal fun listed(
    notification: PaymentNotificationEntity,
    transaction: Transaction? = null,
    alsoIn: List<AlsoIn> = emptyList(),
    since: Instant? = null,
) = ListedNotification(
    notification = notification,
    outcome = Outcome.entries.first { it.name == notification.outcome },
    why = Why.entries.find { it.name == notification.why },
    transaction = transaction,
    alsoIn = alsoIn,
    since = since,
)

private fun id(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())

private fun category(name: String, color: Color, icon: String) = Category(
    id = CategoryId(id(name)),
    name = NotBlankTrimmedString.unsafe(name),
    color = ColorInt(color.toArgb()),
    icon = IconAsset.unsafe(icon),
    orderNum = 0.0,
)
