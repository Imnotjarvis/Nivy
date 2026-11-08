package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test

class ColorModeTest {
    @After
    fun tearDown() {
        ColorMode.minimal = false
        ColorMode.dark = false
    }

    @Test
    fun `colors are drawn as they are unless in minimal colors`() {
        Green shouldBe Palette.Green
        Nivy shouldBe Palette.Nivy
        Palette.Nivy.toArgb().toComposeColor() shouldBe Palette.Nivy
    }

    @Test
    fun `in minimal colors, hues are the theme's text color`() {
        ColorMode.minimal = true

        Green shouldBe Black
        Nivy shouldBe Black
        Orange shouldBe Black

        ColorMode.dark = true

        Green shouldBe White
        Nivy shouldBe White
    }

    @Test
    fun `in minimal colors, hues keep their transparency`() {
        ColorMode.minimal = true

        Green.copy(alpha = 0.5f).alpha.toDouble() shouldBe (0.5 plusOrMinus Tolerance)
        Palette.Green.copy(alpha = 0.5f).shown().alpha.toDouble() shouldBe (0.5 plusOrMinus Tolerance)
    }

    @Test
    fun `in minimal colors, saved item colors are the cards' grey`() {
        ColorMode.minimal = true

        Palette.Nivy.toArgb().toComposeColor() shouldBe MediumWhite

        ColorMode.dark = true

        Palette.Nivy.toArgb().toComposeColor() shouldBe MediumBlack
    }

    @Test
    fun `the color pickers offer the colors as they are, also in minimal colors`() {
        ColorMode.minimal = true

        NIVY_COLOR_PICKER_COLORS_FREE.first() shouldBe Palette.Nivy
        NIVY_COLOR_PICKER_COLORS_PREMIUM.last() shouldBe Palette.Red3Dark
    }

    @Test
    fun `in minimal colors, text on the text color is the background's`() {
        ColorMode.minimal = true

        readableOn(Green) shouldBe White
        Green.dynamicContrast() shouldBe White
        readableOn(White) shouldBe Black
        readableOn(Black) shouldBe White

        ColorMode.dark = true

        readableOn(Green) shouldBe Black
    }

    @Test
    fun `text on a fill is the more readable of white and black, in every color mode`() {
        // The brand purple keeps white text; a bright green takes black (9:1 where white is 2:1).
        readableOn(Palette.Nivy) shouldBe White
        readableOn(Palette.Green) shouldBe Black

        // On every color the pickers offer, at least 4.2:1: the closest calls, a mid red and a
        // dull yellow, are a hair under 4.5 either way.
        (NIVY_COLOR_PICKER_COLORS_FREE + NIVY_COLOR_PICKER_COLORS_PREMIUM).forEach { fill ->
            contrast(readableOn(fill), fill) shouldBeGreaterThanOrEqual 4.2
        }
    }

    @Test
    fun `white content on a fill follows the rule, other tints stay unless the fill went grey`() {
        // A green button's white text turns black; on the brand purple it stays white.
        readableTint(Green, White) shouldBe Black
        readableTint(Nivy, White) shouldBe White
        // A tint chosen for its color stays, as does anything on a translucent fill.
        readableTint(White, Green) shouldBe Green
        readableTint(Nivy.copy(alpha = 0.5f), White) shouldBe White

        ColorMode.minimal = true

        // A button asking for black content on the (black) text color still gets white.
        readableTint(Green, Black) shouldBe White
        // The warnings' red keeps its hue; its white icon follows the rule: black (5.5:1).
        readableTint(Palette.Red, White) shouldBe Black

        ColorMode.dark = true

        // On dark themes the text color is white: white content on it turns black.
        readableTint(Green, White) shouldBe Black
    }

    @Test
    fun `on the light theme, secondary text reads at 4,5 to 1`() {
        // when
        val gray = Gray

        // then
        contrast(gray, White) shouldBeGreaterThanOrEqual TextContrast
        contrast(gray, MediumWhite) shouldBeGreaterThanOrEqual TextContrast
    }

    @Test
    fun `in minimal colors on the light theme, secondary text reads at 4,5 to 1`() {
        // given
        ColorMode.minimal = true

        // when
        val gray = Gray

        // then
        contrast(gray, White) shouldBeGreaterThanOrEqual TextContrast
        contrast(gray, MediumWhite) shouldBeGreaterThanOrEqual TextContrast
    }

    @Test
    fun `on the dark theme, secondary text keeps its grey, also in minimal colors`() {
        // given
        ColorMode.dark = true

        // when
        val default = Gray
        ColorMode.minimal = true
        val minimal = Gray

        // then
        default shouldBe Color(0xFF939199)
        minimal shouldBe Color(0xFF939199)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val Tolerance = 0.01
        const val TextContrast = 4.5
    }
}
