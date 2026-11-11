package com.nivy.importdata.cashew

import com.nivy.base.model.AccountType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Tag
import com.nivy.data.model.TagAssociation
import com.nivy.data.model.TagId
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.importdata.cashew.LeftOut.Reason
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class CashewPlannerTest {

    @Test
    fun `wallets become accounts, with their currency, color, icon and order`() {
        // when
        val plan = plan {
            wallet("w2", "Dollars", currency = "usd", order = 1)
            wallet("w1", "Bank", currency = "eur", colour = "0xff2196f3", order = 0)
        }

        // then
        plan.accounts.map { Triple(it.name.value, it.asset.code, it.orderNum) } shouldBe listOf(
            Triple("Bank", "EUR", 0.0),
            Triple("Dollars", "USD", 1.0),
        )
        plan.accounts.first().color shouldBe ColorInt(0xff2196f3.toInt())
        // Cashew left it to its theme's color: the picker's second, after the first wallet's turn
        plan.accounts.last().color shouldBe pickerColor(1)
    }

    @Test
    fun `a wallet named as an account in the same currency lands on it`() {
        // given
        val bank = account("Bank", "EUR")

        // when
        val plan = plan(wallet(accounts = listOf(bank))) {
            wallet("w1", " bank ", currency = "eur")
            wallet("w2", "Bank", currency = "usd")
            category("c1", "Dining")
            transaction("t1", -12.0, wallet = "w1")
        }

        // then
        plan.accounts.map { it.name.value to it.asset.code } shouldBe listOf("Bank" to "USD")
        (plan.transactions.single() as Expense).account shouldBe bank.id
    }

    @Test
    fun `a category comes over once by name, and a subcategory's transactions go to its main one, tagged`() {
        // given an expense and an income category of the same name, as Cashew keeps them
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Gifts", icon = "gift.png", order = 0)
            category("c2", "gifts", icon = "gift.png", order = 1)
            category("c3", "Flowers", main = "c1")
            transaction("t1", -30.0, category = "c1", subCategory = "c3")
            transaction("t2", 50.0, category = "c2", date = "2026-10-06T10:00:00Z")
        }

        // then
        val gifts = plan.categories.single()
        gifts.name.value shouldBe "Gifts"
        gifts.icon shouldBe IconAsset.unsafe("gift")
        plan.transactions.map { it.category } shouldBe listOf(gifts.id, gifts.id)
        plan.tags.map { it.name.value } shouldBe listOf("Flowers")
        plan.tagLinks shouldBe listOf(
            TagAssociation(plan.tags.single().id, AssociationId(plan.transactions.first().id.value))
        )
    }

    @Test
    fun `a category or a tag the wallet has already is used, not made again`() {
        // given
        val dining = category("Dining")
        val coffee = Tag(TagId(UUID.randomUUID()), NotBlankTrimmedString.unsafe("Coffee"), Instant.EPOCH)

        // when
        val plan = plan(wallet(categories = listOf(dining), tags = listOf(coffee))) {
            wallet("w1", "Bank")
            category("c1", "dining")
            category("c2", "coffee", main = "c1")
            transaction("t1", -3.0, subCategory = "c2")
        }

        // then
        plan.categories.shouldBeEmpty()
        plan.tags.shouldBeEmpty()
        plan.transactions.single().category shouldBe dining.id
        plan.tagLinks.single().id shouldBe coffee.id
    }

    @Test
    fun `paid transactions come over with their amount, time, title and note`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Dining")
            transaction("t1", -4.125, name = "Lunch", note = "With Anna", date = "2026-10-05T13:00:00Z")
            transaction("t2", 2000.0, name = "Salary", date = "2026-10-01T09:00:00Z")
        }

        // then
        with(plan.transactions[0] as Income) {
            value.amount.value shouldBe 2000.0
            value.asset shouldBe AssetCode.EUR
            title?.value shouldBe "Salary"
            time shouldBe Instant.parse("2026-10-01T09:00:00Z")
        }
        with(plan.transactions[1] as Expense) {
            value.amount.value shouldBe 4.125
            title?.value shouldBe "Lunch"
            description?.value shouldBe "With Anna"
            time shouldBe Instant.parse("2026-10-05T13:00:00Z")
            settled shouldBe true
        }
        plan.found shouldBe 2
        plan.imported shouldBe 2
    }

    @Test
    fun `what Cashew doesn't count stays out, and what can't come over is listed`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Dining")
            transaction("t1", -10.0, name = "Skipped", skipped = true, paid = false)
            transaction("t2", -10.0, name = "Not paid", paid = false, date = "2026-10-07T10:00:00Z")
            transaction("t3", 0.0, name = "Free coffee", date = "2026-10-08T10:00:00Z")
            transaction("t4", -10.0, name = "Elsewhere", wallet = "gone", date = "2026-10-09T10:00:00Z")
        }

        // then
        plan.transactions.shouldBeEmpty()
        plan.leftOut shouldBe listOf(
            LeftOut(Reason.NotPaid, listOf(LeftOut.Item("Not paid", LocalDate.of(2026, 10, 7)))),
            LeftOut(Reason.NoAmount, listOf(LeftOut.Item("Free coffee", LocalDate.of(2026, 10, 8)))),
            LeftOut(Reason.NoAccount, listOf(LeftOut.Item("Elsewhere", LocalDate.of(2026, 10, 9)))),
        )
        plan.found shouldBe 2
        plan.imported shouldBe 0
    }

    @Test
    fun `a transfer is one, with what left one wallet and what arrived in the other`() {
        // given Cashew's two halves, as its transfer writes them
        val plan = plan {
            wallet("w1", "Bank", currency = "eur")
            wallet("w2", "Dollars", currency = "usd")
            transaction(
                "in",
                108.0,
                wallet = "w2",
                category = "0",
                name = "Dollars Transfer In",
                note = "Transferred Balance\nBank → Dollars",
                date = "2026-10-05T10:00:31Z",
            )
            transaction(
                "out",
                -100.0,
                wallet = "w1",
                category = "0",
                paired = "in",
                name = "Bank Transfer Out",
                note = "Transferred Balance\nBank → Dollars",
                date = "2026-10-05T10:00:30Z",
            )
        }

        // then
        val bank = plan.accounts.first { it.name.value == "Bank" }
        val dollars = plan.accounts.first { it.name.value == "Dollars" }
        with(plan.transactions.single() as Transfer) {
            fromAccount shouldBe bank.id
            fromValue.amount.value shouldBe 100.0
            toAccount shouldBe dollars.id
            toValue.amount.value shouldBe 108.0
            toValue.asset.code shouldBe "USD"
            time shouldBe Instant.parse("2026-10-05T10:00:30Z")
            // Cashew's own titles and note
            title shouldBe null
            description shouldBe null
            category shouldBe null
        }
        plan.found shouldBe 2
        plan.imported shouldBe 2
    }

    @Test
    fun `a transfer given a title keeps it`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            wallet("w2", "Savings")
            transaction("in", 50.0, wallet = "w2", category = "0", name = "Holiday fund")
            transaction("out", -50.0, wallet = "w1", category = "0", paired = "in", name = "Holiday fund")
        }

        // then
        plan.transactions.single().title?.value shouldBe "Holiday fund"
    }

    @Test
    fun `a balance correction adjusts the balance, as Nivy's own adjustment`() {
        // given a starting balance and a correction, as Cashew writes them
        val plan = plan {
            wallet("w1", "Bank")
            transaction("t1", 1500.0, category = "0", date = "2026-01-10T09:00:00Z")
            transaction(
                "t2",
                -20.5,
                category = "0",
                note = "Updated Total Balance\nBank: 1,479.50 €",
                date = "2026-10-01T09:00:00Z",
            )
        }

        // then
        plan.transactions.map { it.javaClass.simpleName to it.title?.value } shouldBe listOf(
            "Income" to "Adjust balance",
            "Expense" to "Adjust balance",
        )
        plan.transactions.map { it.category to it.description } shouldBe listOf(null to null, null to null)
        plan.categories.shouldBeEmpty()
    }

    private fun account(name: String, currency: String) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = AssetCode.unsafe(currency),
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 3.0,
    )

    private fun category(name: String) = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(0),
        icon = null,
        orderNum = 0.0,
    )
}
