package com.nivy.wallet.ui.theme.components

import com.nivy.wallet.domain.data.NivyCurrency
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import java.util.Locale

class CurrencySearchTest {
    private val phoneLanguage = Locale.getDefault()

    @After
    fun tearDown() {
        Locale.setDefault(phoneLanguage)
    }

    @Test
    fun `a currency is found by its code or name, whatever the case`() {
        NivyCurrency.getAvailable().matching("eur").map { it.code } shouldContain "EUR"
        NivyCurrency.getAvailable().matching("EURO").map { it.code } shouldContain "EUR"
    }

    @Test
    fun `a currency is found by its code whatever language the phone is in`() {
        // In Turkish, I lowercases to a dotless i: "INR" isn't "inr" there.
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        NivyCurrency.getAvailable().matching("inr").map { it.code } shouldContain "INR"
    }

    @Test
    fun `a blank search lists every currency`() {
        NivyCurrency.getAvailable().matching(" ").size shouldBe NivyCurrency.getAvailable().size
    }
}
