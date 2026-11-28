package com.nivy.domain.notification

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.util.UUID

class NotificationDecisionsTest {
    private val intesa = "test.bank.intesa"
    private val revolut = "test.bank.revolut"
    private val wallet = PaymentNotificationReader.GoogleWallet
    private val now = Instant.parse("2026-11-27T12:04:00Z")
    private val payment = Reading.Money(MoneyWay.Spent, 1250, "EUR", "CONAD", card = "1501")

    @Test
    fun `a payment is added, or would be while adding is off`() {
        decide(posted(intesa, "Hai pagato 12,50 €"), payment) shouldBe Decision.Keep(Outcome.Added)
        decide(posted(intesa, "Hai pagato 12,50 €"), payment, adding = false) shouldBe
            Decision.Keep(Outcome.WouldAdd)
    }

    @Test
    fun `what isn't a payment is ignored or kept as not added, with why`() {
        decide(posted(intesa, "Codice 1234"), Reading.Ignored(NotMoney.Code)) shouldBe
            Decision.Keep(Outcome.Ignored, Why.Code)
        decide(posted(intesa, "Hai ricevuto"), payment.copy(way = MoneyWay.Received)) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.MoneyIn)
        decide(posted(intesa, "Hai inviato"), payment.copy(way = MoneyWay.Sent)) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.SentToPerson)
        decide(posted(intesa, "Prelievo"), payment.copy(way = MoneyWay.Cash)) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.Cash)
        decide(posted(intesa, "12,50 €"), payment.copy(way = null)) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.Unread)
        decide(posted(intesa, "Hai pagato 12,50 $"), payment.copy(currency = "$")) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.OtherCurrency)
        decide(posted(intesa, "Hai pagato 12,50 €"), payment, accountCurrency = null) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.NoAccount)
    }

    @Test
    fun `the same notification again, as when Nivy reconnects, is skipped`() {
        val first = earlier(intesa, "Hai pagato 12,50 €", key = "k1", postTime = now.minusSeconds(300))
        val again = posted(intesa, "Hai pagato 12,50 €", key = "k1", postTime = now.minusSeconds(300))
        decide(again, payment, first) shouldBe Decision.Skip
    }

    @Test
    fun `an app updating its notification about the same amount is the same payment, kept once`() {
        val first = earlier(intesa, "Hai pagato 12,50 €", key = "k1")
        decide(posted(intesa, "Hai pagato 12,50 € presso CONAD", key = "k1"), payment, first) shouldBe Decision.Skip
    }

    @Test
    fun `the same text from the same app within the hour is a likely repeat, not added`() {
        val first = earlier(intesa, "Hai pagato 12,50 €", key = "k1", postTime = now.minusSeconds(1800))
        decide(posted(intesa, "Hai pagato 12,50 €", key = "k2"), payment, first) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.LikelyRepeat, about = first.id)
        // Wallet posting its notification again, after it matched the bank's.
        val bank = earlier(intesa, "Hai pagato 12,50 €")
        val walletFirst = earlier(wallet, "CONAD 12,50 €", key = "w1", outcome = Outcome.SamePayment, about = bank.id)
        decide(posted(wallet, "CONAD 12,50 €", key = "w2"), payment, bank, walletFirst) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.LikelyRepeat, about = walletFirst.id)
    }

    @Test
    fun `Google Wallet and a bank reporting one payment within the hour are the same payment`() {
        val bank = earlier(intesa, "Hai pagato 12,50 €", postTime = now.minusSeconds(570))
        decide(posted(wallet, "CONAD 12,50 €"), payment, bank) shouldBe
            Decision.Keep(Outcome.SamePayment, about = bank.id)
        // Wallet first, the bank after.
        val walletFirst = earlier(wallet, "CONAD 12,50 €", postTime = now.minusSeconds(60))
        decide(posted(intesa, "Hai pagato 12,50 €"), payment, walletFirst) shouldBe
            Decision.Keep(Outcome.SamePayment, about = walletFirst.id)
    }

    @Test
    fun `a card number tells payments apart only when both show one`() {
        val otherCard = earlier(intesa, "Hai pagato 12,50 €", card = "7733")
        decide(posted(wallet, "CONAD 12,50 €"), payment, otherCard) shouldBe Decision.Keep(Outcome.Added)
        val noCard = earlier(revolut, "Hai speso 12,50 €", card = null)
        decide(posted(wallet, "CONAD 12,50 €"), payment, noCard) shouldBe
            Decision.Keep(Outcome.SamePayment, about = noCard.id)
    }

    @Test
    fun `a payment Wallet already matched isn't matched again, and the closest in time is`() {
        val older = earlier(intesa, "Hai pagato 12,50 € da A", postTime = now.minusSeconds(3000))
        val newer = earlier(revolut, "Hai speso 12,50 €", postTime = now.minusSeconds(600))
        decide(posted(wallet, "CONAD 12,50 €"), payment, older, newer) shouldBe
            Decision.Keep(Outcome.SamePayment, about = newer.id)
        val matched = earlier(wallet, "BAR 12,50 €", outcome = Outcome.SamePayment, about = newer.id)
        decide(posted(wallet, "CONAD 12,50 €"), payment, older, newer, matched) shouldBe
            Decision.Keep(Outcome.SamePayment, about = older.id)
    }

    @Test
    fun `two banks' notifications are never the same payment, nor are payments over an hour apart`() {
        val revolutPayment = earlier(revolut, "Hai speso 12,50 €")
        decide(posted(intesa, "Hai pagato 12,50 €"), payment, revolutPayment) shouldBe Decision.Keep(Outcome.Added)
        val walletLongAgo = earlier(wallet, "CONAD 12,50 €", postTime = now.minusSeconds(3700))
        decide(posted(intesa, "Hai pagato 12,50 €"), payment, walletLongAgo) shouldBe Decision.Keep(Outcome.Added)
    }

    @Test
    fun `an undone payment stays undone, its repeats and Wallet's copy ignored`() {
        val undone = earlier(intesa, "Hai pagato 12,50 €", outcome = Outcome.Undone)
        decide(posted(intesa, "Hai pagato 12,50 €", key = "k2"), payment, undone) shouldBe
            Decision.Keep(Outcome.Ignored, Why.UndoneBefore, about = undone.id)
        decide(posted(wallet, "CONAD 12,50 €"), payment, undone) shouldBe
            Decision.Keep(Outcome.Ignored, Why.UndoneBefore, about = undone.id)
    }

    @Test
    fun `a payment entered by hand in the hour before isn't added again`() {
        val byHand = HandEntered(cents = 1250, currency = "EUR", time = now.minusSeconds(240))
        decide(posted(intesa, "Hai pagato 12,50 €"), payment, handEntered = listOf(byHand)) shouldBe
            Decision.Keep(Outcome.NotAdded, Why.AddedByHand, at = byHand.time)
        val otherAmount = byHand.copy(cents = 990)
        decide(posted(intesa, "Hai pagato 12,50 €"), payment, handEntered = listOf(otherAmount)) shouldBe
            Decision.Keep(Outcome.Added)
    }

    private fun decide(
        posted: Posted,
        reading: Reading,
        vararg earlier: Earlier,
        handEntered: List<HandEntered> = emptyList(),
        accountCurrency: String? = "EUR",
        adding: Boolean = true,
    ) = NotificationDecisions.decide(
        posted = posted,
        reading = reading,
        earlier = earlier.toList(),
        handEntered = handEntered,
        accountCurrency = accountCurrency,
        adding = adding,
    )

    private fun posted(packageName: String, text: String, key: String = "new", postTime: Instant = now) =
        Posted(packageName = packageName, key = key, postTime = postTime, text = text)

    @Suppress("LongParameterList")
    private fun earlier(
        packageName: String,
        text: String,
        key: String = "k${UUID.randomUUID()}",
        postTime: Instant = now.minusSeconds(120),
        card: String? = "1501",
        outcome: Outcome = Outcome.Added,
        about: UUID? = null,
    ) = Earlier(
        id = UUID.randomUUID(),
        packageName = packageName,
        key = key,
        postTime = postTime,
        text = text,
        cents = 1250,
        currency = "EUR",
        card = card,
        outcome = outcome,
        about = about,
    )
}
