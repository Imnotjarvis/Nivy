package com.nivy.importdata.cashew

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import java.time.Instant

class CashewReaderTest {
    private val files = mutableListOf<CashewFile>()

    private fun cashew(version: Int = 46) = CashewFile(version).also { files += it }

    @After
    fun tearDown() = files.forEach(CashewFile::close)

    @Test
    fun `a data file reads as Cashew wrote it`() {
        // given
        val file = cashew().apply {
            wallet("w1", "Bank", currency = "eur", colour = "0xff2196f3", order = 1)
            category("c1", "Dining", colour = "0xffe91e63", icon = "cutlery.png", order = 2)
            category("c2", "Coffee", main = "c1")
            transaction(
                pk = "t1",
                amount = -4.125,
                name = "Lunch",
                note = "With Anna",
                subCategory = "c2",
                date = "2026-10-05T13:00:00Z",
                dueDate = "2026-10-04T00:00:00Z",
                goal = "g1",
            )
            transaction(
                pk = "t2",
                amount = -9.99,
                paid = false,
                type = CashewType.Subscription,
                repeat = CashewRepeat.Monthly,
                repeatEvery = 1,
                endDate = "2027-03-01T00:00:00Z",
            )
            objective("g1", "Holiday")
            objective("l1", "Car", loan = true, income = true)
            budget(
                "b1",
                "Food",
                300.0,
                wallets = """["w1"]""",
                categories = """["c1"]""",
                excluded = """[]""",
            )
            categoryLimit("cl1", budget = "b1", category = "c1")
            settings(
                """{"selectedWalletPk":"w1","cachedCurrencyExchange":{"eur":0.92,"usd":1},""" +
                    """"customCurrencyAmounts":{"eur":0.9},"other":[1,2]}"""
            )
        }

        // when
        val data = readCashew(file.tables)

        // then
        data.wallets shouldBe listOf(
            CashewWallet(pk = "w1", name = "Bank", colour = "0xff2196f3", iconName = null, order = 1, currency = "eur")
        )
        data.categories shouldBe listOf(
            CashewCategory("c1", "Dining", "0xffe91e63", "cutlery.png", order = 2, mainCategoryPk = null),
            CashewCategory("c2", "Coffee", "0xff4caf50", "cutlery.png", order = 0, mainCategoryPk = "c1"),
        )
        data.transactions.first() shouldBe CashewTransaction(
            pk = "t1", pairedPk = null, name = "Lunch", amount = -4.125, note = "With Anna", categoryPk = "c1",
            subCategoryPk = "c2", walletPk = "w1", date = Instant.parse("2026-10-05T13:00:00Z"), modified = null,
            dueDate = Instant.parse("2026-10-04T00:00:00Z"), repeatEvery = null, repeat = null, endDate = null,
            type = null, paid = true, skipped = false, goalPk = "g1", loanPk = null,
        )
        with(data.transactions.last()) {
            type shouldBe CashewType.Subscription
            repeat shouldBe CashewRepeat.Monthly
            repeatEvery shouldBe 1
            paid shouldBe false
            endDate shouldBe Instant.parse("2027-03-01T00:00:00Z")
        }
        data.objectives.map { Triple(it.name, it.loan, it.income) } shouldBe
            listOf(Triple("Holiday", false, false), Triple("Car", true, true))
        with(data.budgets.single()) {
            walletPks shouldBe listOf("w1")
            categoryPks shouldBe listOf("c1")
            excludedCategoryPks shouldBe null
            repeat shouldBe CashewRepeat.Monthly
        }
        data.limitedBudgets shouldBe setOf("b1")
        data.settings shouldBe CashewSettings(
            primaryWalletPk = "w1",
            rates = mapOf("eur" to 0.92, "usd" to 1.0),
            customRates = mapOf("eur" to 0.9),
        )
        data.settings.rate("eur") shouldBe 0.9
    }

    @Test
    fun `a file from an older Cashew reads without the columns it didn't have yet`() {
        // given version 44: no pairs for transfers, no long-term loans
        val file = cashew(version = 44).apply {
            wallet("w1", "Bank")
            category("c1", "Dining")
            transaction(pk = "t1", amount = -10.0, paired = "t2", loan = "l1")
            objective("l1", "Car", loan = true)
        }

        // when
        val data = readCashew(file.tables)

        // then
        data.transactions.single().pairedPk shouldBe null
        data.transactions.single().loanPk shouldBe null
        data.objectives.single().loan shouldBe false
    }

    @Test
    fun `settings it can't read leave the import without rates`() {
        // given
        val file = cashew().apply {
            wallet("w1", "Bank")
            settings("not json")
        }

        // when
        val settings = readCashew(file.tables).settings

        // then
        settings shouldBe CashewSettings(primaryWalletPk = null, rates = emptyMap(), customRates = emptyMap())
    }

    @Test
    fun `a database without wallets isn't Cashew's`() {
        // given
        val file = cashew()

        // then
        shouldThrow<NotCashewData> { readCashew(file.tables) }
    }
}
