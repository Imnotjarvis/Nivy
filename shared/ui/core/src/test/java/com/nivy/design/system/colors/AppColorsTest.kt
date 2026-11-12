package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nivy.base.model.ColorPalette
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test

/**
 * The palettes' colors as drawn, and the contrast every palette keeps (WCAG 2): 4.5:1 for text,
 * 3:1 for lines, marks and the edges of what's filled.
 */
class AppColorsTest {
    @After
    fun tearDown() {
        ColorMode.palette = ColorPalette.NIVY
        ColorMode.wallpaper = null
        ColorMode.minimal = false
        ColorMode.dark = false
    }

    @Test
    fun `the app's own colors come from the palette chosen`() {
        ColorMode.palette = ColorPalette.OCEAN

        Nivy shouldBe OceanColors.accent.light
        Green shouldBe OceanColors.second.light
        NivyLight shouldBe OceanColors.accentLight

        ColorMode.dark = true

        Orange shouldBe OceanColors.third.dark
        Blue shouldBe OceanColors.fourth.dark
    }

    @Test
    fun `in minimal colors they are the text color, whatever the palette`() {
        ColorMode.palette = ColorPalette.SUNSET
        ColorMode.minimal = true

        Nivy shouldBe Black
        Blue shouldBe Black
        upcomingOrange(White) shouldBe Black
    }

    @Test
    fun `the wallpaper's palette is Nivy's until Android gives its colors`() {
        ColorMode.palette = ColorPalette.WALLPAPER

        Nivy shouldBe NivyColors.accent.light

        ColorMode.wallpaper = SunsetColors

        Nivy shouldBe SunsetColors.accent.light
    }

    @Test
    fun `content on the accent and the second color reads at 4,5 to 1, in every palette`() = everyPalette {
        for (fill in listOf(Nivy, Green)) {
            contrast(readableOn(fill), fill) shouldBeGreaterThanOrEqual TextContrast
        }
    }

    @Test
    fun `the accent and the second color stand 3 to 1 on the light cards and on the dark page`() = everyPalette {
        val backgrounds = if (ColorMode.dark) listOf(Black, BackgroundDark, TrueBlack) else listOf(MediumWhite)
        for (fill in listOf(Nivy, Green)) {
            for (background in backgrounds) {
                contrast(fill, background) shouldBeGreaterThanOrEqual MarkContrast
            }
        }
    }

    // White text at 4.5:1 and 3:1 on the dark cards' grey can't both hold: they stand as far out
    // as white text on them allows, 2.95:1 and over.
    @Test
    fun `on the dark cards, the accent stands out as far as white text on it allows`() = everyPalette {
        if (ColorMode.dark) contrast(Nivy, MediumBlack) shouldBeGreaterThanOrEqual WhiteTextLimitOnDarkCards
    }

    @Test
    fun `chart lines and marks stand 3 to 1 on their theme's cards, in every palette`() = everyPalette {
        val card = if (ColorMode.dark) MediumBlack else MediumWhite
        for (mark in listOf(Orange, Blue)) {
            contrast(mark, card) shouldBeGreaterThanOrEqual MarkContrast
        }
    }

    @Test
    fun `payments to come and tags read at 4,5 to 1, in every palette`() = everyPalette {
        val backgrounds = if (ColorMode.dark) {
            listOf(Black, MediumBlack, TrueBlack)
        } else {
            listOf(White, MediumWhite, BackgroundLight)
        }
        for (background in backgrounds) {
            contrast(upcomingOrange(background), background) shouldBeGreaterThanOrEqual TextContrast
            contrast(tagBlue(background), background) shouldBeGreaterThanOrEqual TextContrast
        }
    }

    // Every palette but the wallpaper's, whose colors are the phone's, on both themes.
    private fun everyPalette(check: () -> Unit) {
        for (palette in ColorPalette.entries - ColorPalette.WALLPAPER) {
            for (dark in listOf(false, true)) {
                ColorMode.palette = palette
                ColorMode.dark = dark
                withClue("$palette, ${if (dark) "dark" else "light"}") { check() }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val TextContrast = 4.5
        const val MarkContrast = 3.0
        const val WhiteTextLimitOnDarkCards = 2.95
    }
}
