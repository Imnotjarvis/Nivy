package com.ivy.accounts

import androidx.compose.ui.graphics.Color
import com.ivy.data.model.Account
import com.ivy.wallet.data.model.AccountData
import com.ivy.wallet.ui.previewAccount
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class AccountsTotalTest {
    private val bank = previewAccount("Bank", Color.Black, currency = "EUR")
    private val dollars = previewAccount("Dollars", Color.Black, currency = "USD")
    private val savingsBox = previewAccount("Savings box", Color.Black, currency = "EUR", includeInBalance = false)

    @Test
    fun `the balances add up in the base currency, the excluded accounts on request`() {
        val accounts = listOf(
            data(bank, balance = 1_000.0),
            data(dollars, balance = 200.0, inBaseCurrency = 180.0),
            data(savingsBox, balance = 5_000.0),
        )

        accounts.totalIn("EUR", withExcluded = false) shouldBe 1_180.0
        accounts.totalIn("EUR", withExcluded = true) shouldBe 6_180.0
    }

    @Test
    fun `an account with no rate to the base currency counts as nothing`() {
        val accounts = listOf(
            data(bank, balance = 100.0),
            data(previewAccount("Yen", Color.Black, currency = "JPY"), balance = 50_000.0, inBaseCurrency = null),
        )

        accounts.totalIn("EUR", withExcluded = true) shouldBe 100.0
    }

    @Test
    fun `cents add up exactly`() {
        val accounts = listOf(data(bank, balance = 0.1), data(bank.copy(name = bank.name), balance = 0.2))

        accounts.totalIn("EUR", withExcluded = true) shouldBe 0.3
    }

    private fun data(account: Account, balance: Double, inBaseCurrency: Double? = null) = AccountData(
        account = account,
        balance = balance,
        balanceBaseCurrency = inBaseCurrency,
        monthlyExpenses = 0.0,
        monthlyIncome = 0.0,
        creditCards = persistentListOf(),
    )
}
