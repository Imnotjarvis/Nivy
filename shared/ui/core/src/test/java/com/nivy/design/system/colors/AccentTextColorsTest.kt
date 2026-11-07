package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.Test

class AccentTextColorsTest {
    @Test
    fun `on light backgrounds a payment to come and tags read at 4,5 to 1`() {
        for (background in listOf(White, MediumWhite, BackgroundLight)) {
            contrast(upcomingOrange(background), background) shouldBeGreaterThanOrEqual TextContrast
            contrast(tagBlue(background), background) shouldBeGreaterThanOrEqual TextContrast
        }
    }

    @Test
    fun `on dark backgrounds they are the palette's own orange and light blue`() {
        for (background in listOf(Black, MediumBlack, TrueBlack)) {
            upcomingOrange(background) shouldBe Palette.Orange
            tagBlue(background) shouldBe Palette.BlueLight
        }
    }

    @Test
    fun `in minimal colors they are the text color`() {
        ColorMode.minimal = true
        try {
            upcomingOrange(White) shouldBe Black
            tagBlue(MediumWhite) shouldBe Black
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
