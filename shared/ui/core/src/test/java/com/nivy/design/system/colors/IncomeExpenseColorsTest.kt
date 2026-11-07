package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.Test

class IncomeExpenseColorsTest {
    @Test
    fun `on light backgrounds income and expenses read at 4,5 to 1`() {
        for (background in listOf(White, MediumWhite, BackgroundLight)) {
            contrast(incomeGreen(background), background) shouldBeGreaterThanOrEqual TextContrast
            contrast(expenseRed(background), background) shouldBeGreaterThanOrEqual TextContrast
        }
    }

    @Test
    fun `on dark backgrounds they are the palette's own green and red`() {
        for (background in listOf(Black, MediumBlack, TrueBlack)) {
            incomeGreen(background) shouldBe Palette.Green
            expenseRed(background) shouldBe Palette.Red
        }
    }

    @Test
    fun `they keep their color in minimal colors`() {
        ColorMode.minimal = true
        try {
            incomeGreen(Black) shouldBe Palette.Green
            expenseRed(White) shouldBe expenseRed(MediumWhite)
            (incomeGreen(White).red == incomeGreen(White).green) shouldBe false
        } finally {
            ColorMode.minimal = false
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val TextContrast = 4.5
    }
}
