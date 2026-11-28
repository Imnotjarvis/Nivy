package com.nivy.paymentnotifications

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.domain.notification.AlsoIn
import com.nivy.domain.notification.MoneyWay
import com.nivy.domain.notification.Outcome
import com.nivy.domain.notification.Totals
import com.nivy.domain.notification.Why
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * Payment notifications' pages: the first time and once set up, Android's restricted settings,
 * keeping Nivy listening, the apps to pick, the log, and the list of what was added.
 */
@RunWith(TestParameterInjector::class)
class PaymentNotificationsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    // Notification access off, the three steps, adding off.
    @Test
    fun `snapshot the page the first time`() = shot { UI(state = FirstTime, onEvent = {}) }

    // From Android 13, for an app installed from a file.
    @Test
    fun `snapshot the steps to allow restricted settings`() = shot {
        UI(state = FirstTime, onEvent = {}, restrictedStepsShown = true)
    }

    // Listening, adding on, the apps and where their money comes from, then the cards.
    @Test
    fun `snapshot the page once set up`() = shot { UI(state = SetUp, onEvent = {}) }

    // Android limits Nivy's battery; the test not run yet.
    @Test
    fun `snapshot keeping Nivy listening, before the steps`() = shot {
        KeepListeningUI(state = KeepListeningState(batteryUnrestricted = false, test = null), onEvent = {})
    }

    @Test
    fun `snapshot keeping Nivy listening, once its test arrived`() = shot {
        KeepListeningUI(state = KeepListeningState(batteryUnrestricted = true, test = TestResult.Working), onEvent = {})
    }

    // The apps picked first, with where their money comes from, then the rest by name.
    @Test
    fun `snapshot the apps to pick`() = shot { NotificationAppsUI(state = Apps, onEvent = {}) }

    // Trying it out: what Nivy would add, a payment Wallet reported too, money received, a hold, a code.
    @Test
    fun `snapshot the log while trying it out`() = shot { NotificationLogUI(state = Log, onEvent = {}) }

    @Test
    fun `snapshot a notification opened in the log`() = shot {
        NotificationLogUI(state = Log.copy(opened = Log.entries.first()), onEvent = {})
    }

    // A payment Wallet reported too, a card's account to check, one without a shop's name, one
    // undone; then money received, money sent and a likely repeat, their buttons' texts centered.
    @Test
    fun `snapshot the list of what was added and what wasn't`() = shot {
        AddedFromNotificationsUI(state = AddedList, onEvent = {})
    }

    private fun shot(content: @Composable BoxWithConstraintsScope.() -> Unit) =
        snapshot(theme, minimal = colors == Colors.Minimal) {
            val dark = theme == PaparazziTheme.Dark
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = FixedTime) {
                content()
            }
        }
}

internal val FirstTime = PaymentNotificationsState(
    granted = false,
    listening = false,
    lastPayment = null,
    test = null,
    adding = false,
    apps = persistentListOf(),
    cards = persistentListOf(),
    batteryUnrestricted = false,
    installedFromFile = true,
    payer = null,
    accounts = SampleBaseData.accounts,
    creditCards = persistentListOf(),
)

private fun payer(kind: String, id: String, name: String) =
    Payer(kind = kind, id = id, name = name, account = Intesa.id, creditCard = null, picked = true)

internal val SetUp = FirstTime.copy(
    granted = true,
    listening = true,
    lastPayment = at(28, 12, 5),
    adding = true,
    batteryUnrestricted = true,
    apps = persistentListOf(
        PickedApp(payer(PaymentSourceEntity.App, Wallet, "Google Wallet"), "Intesa", wallet = true, at(28, 12, 5)),
        PickedApp(payer(PaymentSourceEntity.App, IntesaApp, "Intesa Sanpaolo"), "Intesa", false, at(28, 12, 4)),
        PickedApp(payer(PaymentSourceEntity.App, RevolutApp, "Revolut"), "Revolut", wallet = false, at(28, 10, 15)),
        PickedApp(payer(PaymentSourceEntity.App, "it.satispay", "Satispay"), "Revolut", wallet = false, last = null),
    ),
    cards = persistentListOf(
        KnownCard(payer(PaymentSourceEntity.Card, "1501", "1501"), "Intesa"),
        KnownCard(payer(PaymentSourceEntity.Card, "7733", "7733"), "Revolut"),
    ),
)

internal val Apps = NotificationAppsState(
    apps = (
        listOf(
            AppChoice(payer(PaymentSourceEntity.App, Wallet, "Google Wallet"), "Intesa"),
            AppChoice(payer(PaymentSourceEntity.App, IntesaApp, "Intesa Sanpaolo"), "Intesa"),
            AppChoice(payer(PaymentSourceEntity.App, RevolutApp, "Revolut"), "Revolut"),
        ) + listOf("BBVA", "Calendar", "Fineco", "Gmail", "HYPE", "Maps", "PayPal", "Satispay").map { name ->
            AppChoice(
                Payer(PaymentSourceEntity.App, "app.$name", name, account = null, creditCard = null, picked = false),
                paysFrom = null,
            )
        }
        ).toImmutableList(),
    payer = null,
    accounts = SampleBaseData.accounts,
    creditCards = persistentListOf(),
)

private fun entry(
    app: String,
    time: Instant,
    title: String?,
    text: String,
    outcome: Outcome,
    why: Why? = null,
    amount: String? = null,
    merchant: String? = null,
    card: String? = null,
) = LogEntry(
    id = UUID.nameUUIDFromBytes("$app $time".toByteArray()),
    packageName = "app.$app",
    app = app,
    time = time,
    title = title,
    text = text,
    outcome = outcome,
    why = why,
    amount = amount,
    merchant = merchant,
    card = card,
    cardPaysFrom = card?.let { "Intesa" },
    sameAs = if (outcome == Outcome.SamePayment) "Intesa Sanpaolo" else null,
    sameAt = if (outcome == Outcome.SamePayment) at(28, 12, 4) else null,
)

internal val Log = NotificationLogState(
    entries = persistentListOf(
        entry(
            "Google Wallet", at(28, 12, 5), "CONAD SUPERSTORE", "12,50 € con Carta Visa ••1501",
            Outcome.SamePayment, amount = "12.50 EUR", merchant = "CONAD SUPERSTORE", card = "1501",
        ),
        entry(
            "Intesa Sanpaolo", at(28, 12, 4), "Intesa Sanpaolo",
            "Hai pagato 12,50 € con la carta *1501 da CONAD SUPERSTORE",
            Outcome.WouldAdd, amount = "12.50 EUR", merchant = "CONAD SUPERSTORE", card = "1501",
        ),
        entry(
            "Revolut", at(28, 10, 15), "Hai ricevuto 57 €", "Pagamento ricevuto da Marco",
            Outcome.NotAdded, Why.MoneyIn, amount = "57.00 EUR", merchant = "Marco",
        ),
        entry(
            "Revolut", at(28, 8, 31), "Vega Carburanti", "Hai pagato 10 € presso Vega Carburanti",
            Outcome.WouldAdd, amount = "10.00 EUR", merchant = "Vega Carburanti",
        ),
        entry("Revolut", at(28, 8, 29), "Vega Carburanti", "34,53 € temporary hold", Outcome.Ignored, Why.Hold),
        entry(
            "Intesa Sanpaolo", at(27, 21, 10), "Intesa Sanpaolo", "Codice 4829 per autorizzare il pagamento",
            Outcome.Ignored, Why.Code,
        ),
    ),
    trying = true,
    opened = null,
)

private val Conad = added(at(28, 12, 4), "CONAD SUPERSTORE", Groceries, 12.50)
private val Vega = added(at(28, 8, 31), "Vega Carburanti", Transport, 10.0, Revolut)
private val BarRoma = added(at(27, 19, 42), "Bar Roma", EatingOut, 4.20)
private val NoShop = added(at(27, 13, 5), title = null, category = null, amount = 23.99, account = Revolut)

internal val AddedList = AddedFromNotificationsState(
    added = persistentListOf(
        listed(
            read(
                IntesaApp, "Intesa Sanpaolo", at(28, 12, 4), "Intesa Sanpaolo", "Hai pagato 12,50 € da CONAD",
                Outcome.Added, cents = 1250, merchant = "CONAD SUPERSTORE", transaction = Conad,
            ),
            transaction = Conad,
            alsoIn = listOf(AlsoIn(UUID.nameUUIDFromBytes("Wallet".toByteArray()), "Google Wallet")),
        ),
        listed(
            read(
                RevolutApp, "Revolut", at(28, 8, 31), "Vega Carburanti", "Hai pagato 10 €",
                Outcome.Added, cents = 1000, merchant = "Vega Carburanti", transaction = Vega,
            ),
            transaction = Vega,
        ),
        listed(
            read(
                Wallet, "Google Wallet", at(27, 19, 42), "Bar Roma", "4,20 € con Carta Visa ••1501",
                Outcome.Added, cents = 420, merchant = "Bar Roma", card = "1501", transaction = BarRoma, guessed = true,
            ),
            transaction = BarRoma,
        ),
        listed(
            read(
                RevolutApp, "Revolut", at(27, 13, 5), "Revolut", "Hai pagato 23,99 €",
                Outcome.Added, cents = 2399, transaction = NoShop,
            ),
            transaction = NoShop,
        ),
        listed(
            read(
                IntesaApp, "Intesa Sanpaolo", at(27, 11, 20), "Zalando", "Hai pagato 49,90 €",
                Outcome.Undone, cents = 4990, merchant = "Zalando",
            ),
        ),
    ),
    notAdded = persistentListOf(
        listed(
            read(
                RevolutApp, "Revolut", at(28, 10, 15), "Hai ricevuto 57 €", "Pagamento ricevuto da Marco",
                Outcome.NotAdded, Why.MoneyIn, MoneyWay.Received, cents = 5700, merchant = "Marco",
            ),
        ),
        listed(
            read(
                IntesaApp, "Intesa Sanpaolo", at(28, 12, 40), "Intesa Sanpaolo",
                "Hai pagato 12,50 € con la carta *1501 da CONAD SUPERSTORE",
                Outcome.NotAdded, Why.LikelyRepeat, cents = 1250,
            ),
            since = at(28, 12, 4),
        ),
        listed(
            read(
                RevolutApp, "Revolut", at(27, 20, 2), "Denaro inviato", "Hai inviato 28 € a Giulia",
                Outcome.NotAdded, Why.SentToPerson, MoneyWay.Sent, cents = 2800, merchant = "Giulia",
            ),
        ),
    ),
    totals = Totals(income = 0.0, expenses = 50.69),
    since = at(27, 11, 20),
    baseData = SampleBaseData,
)
