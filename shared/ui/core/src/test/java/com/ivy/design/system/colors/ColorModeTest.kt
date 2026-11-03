package com.ivy.design.system.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test

class ColorModeTest {
    @After
    fun tearDown() {
        ColorMode.minimal = false
    }

    @Test
    fun `a color's grey has its lightness`() {
        for (color in listOf(Palette.Green, Palette.Ivy, Palette.Yellow, Palette.RedDark)) {
            val grey = color.toGrey()

            grey.red.toDouble() shouldBe (grey.green.toDouble() plusOrMinus Tolerance)
            grey.green.toDouble() shouldBe (grey.blue.toDouble() plusOrMinus Tolerance)
            grey.luminance().toDouble() shouldBe (color.luminance().toDouble() plusOrMinus Tolerance)
        }
    }

    @Test
    fun `white, black and transparency stay`() {
        Color.White.toGrey().red.toDouble() shouldBe (1.0 plusOrMinus Tolerance)
        Color.Black.toGrey().red.toDouble() shouldBe (0.0 plusOrMinus Tolerance)
        Palette.Green.copy(alpha = 0.5f).toGrey().alpha.toDouble() shouldBe (0.5 plusOrMinus Tolerance)
    }

    @Test
    fun `colors are drawn as they are unless in minimal colors`() {
        Green shouldBe Palette.Green
        GradientIvy.startColor shouldBe Palette.Ivy
        Palette.Ivy.toArgb().toComposeColor() shouldBe Palette.Ivy

        ColorMode.minimal = true

        Green shouldBe Palette.Green.toGrey()
        GradientIvy.startColor shouldBe Palette.Ivy.toGrey()
        Palette.Ivy.toArgb().toComposeColor() shouldBe Palette.Ivy.toGrey()
    }

    @Test
    fun `the color pickers offer the colors as they are, also in minimal colors`() {
        ColorMode.minimal = true

        IVY_COLOR_PICKER_COLORS_FREE.first() shouldBe Palette.Ivy
        IVY_COLOR_PICKER_COLORS_PREMIUM.last() shouldBe Palette.Red3Dark
    }

    @Test
    fun `in minimal colors, text on grey is the more readable of black and white`() {
        ColorMode.minimal = true

        // The green account's grey is mid-light: white on it would be about 2:1, black about 10:1.
        findContrastTextColor(Green) shouldBe Black
        Green.dynamicContrast() shouldBe Black
        findContrastTextColor(IvyDark) shouldBe White
        readableOn(White) shouldBe Black
        readableOn(Black) shouldBe White
    }

    private companion object {
        const val Tolerance = 0.01
    }
}
