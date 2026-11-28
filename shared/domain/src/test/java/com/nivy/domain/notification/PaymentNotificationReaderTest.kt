package com.nivy.domain.notification

import com.nivy.domain.notification.MoneyWay.Cash
import com.nivy.domain.notification.MoneyWay.Received
import com.nivy.domain.notification.MoneyWay.Sent
import com.nivy.domain.notification.MoneyWay.Spent
import com.nivy.domain.notification.Why.Access
import com.nivy.domain.notification.Why.Code
import com.nivy.domain.notification.Why.Declined
import com.nivy.domain.notification.Why.Hold
import com.nivy.domain.notification.Why.LowBalance
import com.nivy.domain.notification.Why.NoAmount
import com.nivy.domain.notification.Why.NotYet
import com.nivy.domain.notification.Why.Offer
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.Test

/**
 * The reader, on notifications of many apps. Revolut's and the ING one are real; Google Wallet's
 * follow its known layout, from Spanish and Italian projects' tests; the rest are made up in the
 * shapes Italian banks use, so they're marked "made up". Real ones from the phone join them as the
 * log shows them.
 */
class PaymentNotificationReaderTest {

    @Test
    fun `Revolut's captured payments, transfers and pushes that aren't payments`() = expect(
        // Captured on an Android phone: a no-break space before each €, a balance line after.
        revolut("Vega Carburanti", "Hai pagato 10 € presso Vega Carburanti ⛽\n⚠️ Saldo: 24,53 €")
            to money(Spent, 1000, "Vega Carburanti"),
        // The shop only in the title, and the balance after.
        revolut("Pizzeria Rosticceria N", "🍕 Hai speso 17 €\nSaldo di EUR: 1.722,83 €")
            to money(Spent, 1700, "Pizzeria Rosticceria N"),
        // A transfer in: the amount in the title, the payer in the text.
        revolut("Hai ricevuto 57 €", "Pagamento ricevuto da Persona di test. Premi per i dettagli")
            to money(Received, 5700, "Persona di test"),
        revolut("Denaro inviato ✅", "Hai inviato 28 € a Persona di test. Arriverà in pochi secondi")
            to money(Sent, 2800, "Persona di test"),
        // One fuel payment's other three pushes: a hold, its update and a low balance, in two languages.
        revolut("Vega Carburanti", "34,53 € temporary hold") to Reading.Ignored(Hold),
        revolut("Vega Carburanti", "Pagamento aggiornato da 34,53 € a 10 €") to Reading.Ignored(Hold),
        revolut("Revolut", "Saldo inferiore a €30") to Reading.Ignored(LowBalance),
        revolut(
            "Audi Revolut F1® Team: Brasile",
            "Vantaggi del valore di 4.100 € gratis. Si applicano i Termini e condizioni",
        ) to Reading.Ignored(Offer),
        revolut("Pagamento in sospeso", "Hai inviato 28 € a Persona. Pagamento in sospeso")
            to Reading.Ignored(NotYet),
    )

    @Test
    fun `Google Wallet's payments have the shop as title and no verb, in any currency`() = expect(
        wallet("Example Market", "24,61 € con Carta Visa ••1501")
            to money(Spent, 2461, "Example Market", card = "1501"),
        wallet("PANIFICIO DI PROVA", "€12,40 with TEST Card Plus ••0000")
            to money(Spent, 1240, "PANIFICIO DI PROVA", card = "0000"),
        // Captured in Colombia: the currency before the amount, then after it since August 2026.
        wallet("PERGAMINO VIVA ENVIGAD", "COP7,525.00 con TARJETA NEGRA RAPPICARD CO ••3679")
            to money(Spent, 752_500, "PERGAMINO VIVA ENVIGAD", currency = "COP", card = "3679"),
        wallet("IKEA ENVIGADO", "216.333 COP con Tarjeta Visa ••3679")
            to money(Spent, 21_633_300, "IKEA ENVIGADO", currency = "COP", card = "3679"),
        wallet("Starbucks", "$23.49 with Mastercard **** 1234") to money(Spent, 2349, "Starbucks", "$", "1234"),
        wallet("CONAD", "Rimborso di 12,50 € su Carta Visa ••1501")
            to money(Received, 1250, "CONAD", card = "1501"),
        wallet("CONAD", "Pagamento rifiutato: 12,50 € con Carta Visa ••1501") to Reading.Ignored(Declined),
        wallet("Your pass is ready", "Boarding pass from MDE to CLO") to Reading.Ignored(NoAmount),
    )

    @Test
    fun `banks' card payments, in the shapes Italian banks use`() = expect(
        // A real ING Italia push: the amount with a point, the shop after a comma.
        bank("ING", "Operazione autorizzata: 98.25 euro, PAYPAL *PAG PUB AMM PA 353143XXXXX. Non sei stato tu?")
            to money(Spent, 9825, "PAYPAL *PAG PUB AMM PA 353143XXXXX"),
        // Made up: the card, the date and time, then the shop and its town, then what's left.
        bank(
            "Intesa Sanpaolo",
            "Hai pagato 24,50 € con la carta *0000 il 12.03 alle ore 14:25 da NEGOZIO DI PROVA, ROMA. " +
                "Hai ancora a disposizione 1234,56€.",
        ) to money(Spent, 2450, "NEGOZIO DI PROVA", card = "0000"),
        // Made up.
        bank("isybank", "Pagamento di 12,50 € con carta *1234 presso CONAD")
            to money(Spent, 1250, "CONAD", card = "1234"),
        bank("UniCredit", "Pagamento con carta di EUR 12,50 presso CONAD SUPERSTORE il 27/11")
            to money(Spent, 1250, "CONAD SUPERSTORE"),
        bank("Fineco", "Addebito carta: 12,50 EUR - CONAD SUPERSTORE") to money(Spent, 1250, "CONAD SUPERSTORE"),
        bank("HYPE", "Pagamento di 12,50 € effettuato presso CONAD") to money(Spent, 1250, "CONAD"),
        bank("BBVA", "Hai effettuato un pagamento di 12,50 € presso CONAD con la carta che termina con 1234")
            to money(Spent, 1250, "CONAD", card = "1234"),
        bank("Nexi Pay", "Autorizzato pagamento di 12,50 EUR presso CONAD SUPERSTORE con carta ****1234")
            to money(Spent, 1250, "CONAD SUPERSTORE", card = "1234"),
        bank("Mediolanum", "Pagamento carta di 12,50 EUR effettuato presso CONAD") to money(Spent, 1250, "CONAD"),
        bank("N26", "You spent €12.50 at Lidl") to money(Spent, 1250, "Lidl"),
        bank("N26", "Hai speso 12,50 € da Lidl") to money(Spent, 1250, "Lidl"),
        bank("Satispay", "Hai pagato 3,50 € a Bar Roma") to money(Spent, 350, "Bar Roma"),
        // The shop in the title, before the amount.
        bank("PayPal", "Il tuo acquisto presso Zalando", "Hai pagato 49,90 € EUR") to money(Spent, 4990, "Zalando"),
    )

    @Test
    fun `amounts in any format, next to their currency, before or after`() = expect(
        bank("Bank", "Hai pagato 1.234,56 € presso Mediaworld") to money(Spent, 123_456, "Mediaworld"),
        bank("Bank", "You paid €1,234.56 to Apple") to money(Spent, 123_456, "Apple"),
        bank("Bank", "Pagati 30 euro presso Benzinaio") to money(Spent, 3000, "Benzinaio"),
        bank("Bank", "Addebito di EUR 7,90 presso Netflix") to money(Spent, 790, "Netflix"),
        bank("Bank", "Hai pagato 15 £ presso Pret A Manger") to money(Spent, 1500, "Pret A Manger", "GBP"),
        bank("Bank", "You spent $23.49 at Starbucks") to money(Spent, 2349, "Starbucks", "$"),
        bank("Bank", "Hai speso 0,5 € presso Parcometro") to money(Spent, 50, "Parcometro"),
        // Four decimals aren't an amount.
        bank("Bank", "Hai pagato 12,3456 € presso Bar") to Reading.Ignored(NoAmount),
        // Only a balance isn't an amount either.
        bank("Bank", "Saldo disponibile: 1.234,56 €") to Reading.Ignored(NoAmount),
    )

    @Test
    fun `money in, money sent to a person, cash and what can't be told`() = expect(
        bank("Bank", "Bonifico ricevuto: 1.250,00 € da ACME SRL") to money(Received, 125_000, "ACME SRL"),
        revolut("Zalando", "Rimborso di 17 €") to money(Received, 1700, "Zalando"),
        bank("HYPE", "Accredito stipendio di 1.850,00 EUR") to money(Received, 185_000, merchant = null),
        bank("Bank", "Hai inviato un bonifico di 200 € a Giulia Bianchi") to money(Sent, 20_000, "Giulia Bianchi"),
        bank("PayPal", "Hai inviato 12,40 € a Mario Rossi") to money(Sent, 1240, "Mario Rossi"),
        bank("Bank", "Prelievo di 50,00 € presso ATM Intesa Sanpaolo") to money(Cash, 5000, "ATM Intesa Sanpaolo"),
        // Milan's transport is called ATM: a payment there isn't cash.
        bank("Bank", "Hai pagato 2 € presso ATM Milano") to money(Spent, 200, "ATM Milano"),
        // An amount and a card, but nothing says which way: not a payment until a person says so.
        bank("Fineco", "Carta *1234: 12,50 EUR") to money(way = null, cents = 1250, merchant = null, card = "1234"),
    )

    @Test
    fun `codes, declines, reminders, offers and access alerts aren't payments`() = expect(
        bank("Bank", "Codice 482913 per autorizzare il pagamento di 23,99 €") to Reading.Ignored(Code),
        bank("Bank", "Il tuo codice OTP è 1234") to Reading.Ignored(Code),
        bank("Bank", "Your One-Time Password for an Amex SafeKey purchase of €49.00 is 123456")
            to Reading.Ignored(Code),
        bank("Bank", "Conferma il pagamento di 12,50 € su Amazon nell'app") to Reading.Ignored(Code),
        bank("Bank", "Pagamento rifiutato: 12,50 € presso CONAD") to Reading.Ignored(Declined),
        bank("Bank", "Il pagamento di 12,50 € non è andato a buon fine") to Reading.Ignored(Declined),
        bank("Bank", "Operazione non autorizzata: 98,25 €") to Reading.Ignored(Declined),
        bank("Bank", "Il 1° dicembre ti verrà addebitato 9,99 € per Spotify") to Reading.Ignored(NotYet),
        bank("Bank", "Promemoria: la rata di 120 € scade domani") to Reading.Ignored(NotYet),
        bank("Bank", "Nuovo accesso da un dispositivo Android") to Reading.Ignored(Access),
    )

    @Test
    fun `words that look like those filters in a payment don't stop it`() = expect(
        // "due" is two in Italian; a discount and a shop called CODE are still payments.
        bank("Bank", "Hai pagato 3 € per due caffè presso Bar Roma") to money(Spent, 300, "Bar Roma"),
        bank("Bank", "Hai pagato 12,50 € con lo sconto del 10% presso CONAD") to money(Spent, 1250, "CONAD"),
        bank("Bank", "Hai pagato 5 € con carta *1234 presso CODE CAFE")
            to money(Spent, 500, "CODE CAFE", card = "1234"),
        // A title with an emoji is a shop without it.
        revolut("☕ Bar Roma", "Hai speso 1,20 €") to money(Spent, 120, "Bar Roma"),
    )

    private fun expect(vararg cases: Pair<NotificationText, Reading>) {
        cases.forEach { (notification, reading) ->
            withClue("${notification.title} / ${notification.text}") {
                PaymentNotificationReader.read(notification) shouldBe reading
            }
        }
    }

    private fun revolut(title: String?, text: String) = NotificationText(
        packageName = "com.revolut.revolut",
        appName = "Revolut",
        title = title,
        text = text,
    )

    private fun wallet(title: String?, text: String) = NotificationText(
        packageName = PaymentNotificationReader.GoogleWallet,
        appName = "Wallet",
        title = title,
        text = text,
    )

    // A bank's app, its name as its title, unless the notification has one of its own.
    private fun bank(app: String, text: String) = bank(app, app, text)

    private fun bank(app: String, title: String?, text: String) = NotificationText(
        packageName = "test.bank.${app.lowercase().filter(Char::isLetter)}",
        appName = app,
        title = title,
        text = text,
    )

    private fun money(
        way: MoneyWay?,
        cents: Long,
        merchant: String?,
        currency: String = "EUR",
        card: String? = null,
    ) = Reading.Money(way = way, cents = cents, currency = currency, merchant = merchant, card = card)
}
