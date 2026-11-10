package com.nivy.widget.balance

import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class WidgetAmountTest {
    private val phoneLanguage = Locale.getDefault()

    @Before
    fun english() {
        Locale.setDefault(Locale.US)
    }

    @After
    fun restore() {
        Locale.setDefault(phoneLanguage)
    }

    @Test
    fun `the widget writes amounts as the app does, shortened from 100k on`() {
        widgetAmount(2_419.5, "EUR") shouldBe "2,419.50"
        widgetAmount(80.5, "EUR") shouldBe "80.50"
        widgetAmount(-1_234.0, "EUR") shouldBe "-1,234.00"
        widgetAmount(123_456.0, "EUR") shouldBe "123.46k"
    }

    @Test
    fun `the chips shorten from 1,000 on, to keep to one line, and write smaller amounts as the app does`() {
        chipAmount(80.5, "EUR") shouldBe "80.50"
        chipAmount(0.0, "EUR") shouldBe "0.00"
        chipAmount(2_500.0, "EUR") shouldBe "2.50k"
        chipAmount(12_345.67, "EUR") shouldBe "12.35k"
    }
}
