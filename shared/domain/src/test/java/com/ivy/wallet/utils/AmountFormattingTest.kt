package com.ivy.wallet.utils

import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class AmountFormattingTest {
    private val defaultLocale = Locale.getDefault()

    @Before
    fun arabicLocale() {
        // Formats numbers with Arabic-Indic digits (١٢٣), as an Arabic Android device does.
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `amounts typed digit by digit keep every digit`() {
        val typed = listOf("1", "2", "3").fold("") { amount, key ->
            formatInputAmount(currency = "USD", amount = amount, newSymbol = key) ?: amount
        }

        typed.amountToDouble() shouldBe 123.0
    }

    @Test
    fun `a calculator expression with localized digits evaluates`() {
        "${formatInt(12)}+3".normalizeExpression() shouldBe "12+3"
    }
}
