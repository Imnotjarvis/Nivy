package com.nivy.wallet.ui.component.notification

import androidx.compose.ui.graphics.Color
import com.nivy.base.model.TransactionType
import com.nivy.data.db.entity.PaymentNotificationEntity
import com.nivy.domain.notification.ListedNotification
import com.nivy.domain.notification.MoneyWay
import com.nivy.domain.notification.Outcome
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PaymentNotificationPartsTest {
    @Test
    fun `a payment added shows its transaction's amount, signed by its type`() {
        val bank = previewAccount("Bank", Color.Blue, currency = "EUR")

        listed(MoneyWay.Spent, Outcome.Added, previewTransaction(TransactionType.EXPENSE, bank, 12.5))
            .signedAmount() shouldBe "-12.50 EUR"
        listed(MoneyWay.Received, Outcome.Added, previewTransaction(TransactionType.INCOME, bank, 57.0))
            .signedAmount() shouldBe "+57.00 EUR"
    }

    @Test
    fun `money received and undone stays an income, with the amount read`() {
        val undone = listed(MoneyWay.Received, Outcome.Undone, transaction = null)

        undone.isIncome() shouldBe true
        undone.signedAmount() shouldBe "+57.00 EUR"
    }

    private fun listed(way: MoneyWay, outcome: Outcome, transaction: com.nivy.data.model.Transaction?) =
        ListedNotification(
            notification = PaymentNotificationEntity(
                id = UUID.randomUUID(),
                packageName = "test.bank",
                appName = "Bank",
                notificationKey = "k",
                postTime = Instant.parse("2026-11-28T12:00:00Z"),
                title = "Bank",
                text = "",
                outcome = outcome.name,
                why = null,
                way = way.name,
                cents = 5700,
                currency = "EUR",
                merchant = null,
                card = null,
                transactionId = transaction?.id?.value,
                about = null,
                aboutTime = null,
                guessedAccount = false,
                reviewed = false,
            ),
            outcome = outcome,
            why = null,
            transaction = transaction,
            alsoIn = emptyList(),
            since = null,
        )
}
