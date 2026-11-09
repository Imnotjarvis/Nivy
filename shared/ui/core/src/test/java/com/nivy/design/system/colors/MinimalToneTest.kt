package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Test

class MinimalToneTest {
    @After
    fun tearDown() {
        ColorMode.dark = false
    }

    @Test
    fun `each tone stands out from the cards' grey, on both themes`() {
        for ((dark, backdrop) in listOf(false to MediumWhite, true to MediumBlack)) {
            ColorMode.dark = dark
            for (rank in 0 until Tones) {
                contrast(minimalTone(rank), backdrop) shouldBeGreaterThanOrEqual GraphicsContrast
            }
        }
    }

    @Test
    fun `neighbouring parts differ and the tones repeat after three`() {
        for (rank in 0 until Tones) {
            minimalTone(rank) shouldNotBe minimalTone(rank + 1)
        }
        minimalTone(Tones) shouldBe minimalTone(0)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val Tones = 3
        const val GraphicsContrast = 3.0
    }
}
