package com.nivy.piechart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.MediumBlack
import com.nivy.design.system.colors.MediumWhite
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Test

class PieToneTest {
    @After
    fun tearDown() {
        ColorMode.dark = false
    }

    @Test
    fun `each slice tone stands out from the chart's backdrop, on both themes`() {
        for ((dark, backdrop) in listOf(false to MediumWhite, true to MediumBlack)) {
            ColorMode.dark = dark
            for (rank in 0 until Tones) {
                contrast(pieTone(rank), backdrop) shouldBeGreaterThanOrEqual GraphicsContrast
            }
        }
    }

    @Test
    fun `neighbouring slices differ and the tones repeat after three`() {
        for (rank in 0 until Tones) {
            pieTone(rank) shouldNotBe pieTone(rank + 1)
        }
        pieTone(Tones) shouldBe pieTone(0)
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
