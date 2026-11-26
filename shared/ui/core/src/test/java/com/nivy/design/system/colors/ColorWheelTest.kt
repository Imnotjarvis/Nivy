package com.nivy.design.system.colors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test

class ColorWheelTest {

    @Test
    fun `a hue's tone runs from almost white through the palette's light, base and dark to almost black`() {
        wheelColor(hue = 200f, tone = 0.25f) shouldBe Color.hsv(200f, 0.3f, 1f)
        wheelColor(hue = 200f, tone = 0.5f) shouldBe Color.hsv(200f, 0.7f, 1f)
        wheelColor(hue = 200f, tone = 0.75f) shouldBe Color.hsv(200f, 0.7f, 0.5f)
        wheelColor(hue = 200f, tone = 0f).luminance().toDouble() shouldBe (0.92 plusOrMinus 0.08)
        wheelColor(hue = 200f, tone = 1f).luminance().toDouble() shouldBe (0.02 plusOrMinus 0.02)
    }

    @Test
    fun `a color made on the wheel is found back where it was made`() {
        for (hue in 0 until 360 step 30) {
            // From a light tone on: at the lightest, the channels are too close for a hue to the degree.
            for (step in 1..10) {
                val tone = step / 10f

                val position = wheelColor(hue.toFloat(), tone).wheelPosition()

                position.hue.toDouble() shouldBe (hue.toDouble() plusOrMinus 2.0)
                position.tone.toDouble() shouldBe (tone.toDouble() plusOrMinus 0.01)
            }
        }
    }

    @Test
    fun `a palette color sits at its hue, at about its light, base or dark tone`() {
        Palette.Blue.wheelPosition().hue.toDouble() shouldBe (200.0 plusOrMinus 1.0)
        Palette.Blue.wheelPosition().tone.toDouble() shouldBe (0.5 plusOrMinus 0.02)
        Palette.BlueLight.wheelPosition().tone.toDouble() shouldBe (0.25 plusOrMinus 0.02)
        Palette.BlueDark.wheelPosition().tone.toDouble() shouldBe (0.75 plusOrMinus 0.02)
    }

    @Test
    fun `the ring of greys runs evenly from a light grey at the top to a near black at the bottom`() {
        val greys = (0..10).map { wheelGrey(it / 10f) }

        greys.forEach { it.red shouldBe it.blue }
        greys.map { it.luminance() } shouldBe greys.map { it.luminance() }.sortedDescending()
        // L* 88 to 12, as the greys the picker offered before.
        greys.first().red.toDouble() shouldBe (0.86 plusOrMinus 0.01)
        greys.last().red.toDouble() shouldBe (0.12 plusOrMinus 0.01)
        greys.forEachIndexed { index, grey ->
            grey.wheelGreyShare().toDouble() shouldBe (index / 10.0 plusOrMinus 0.01)
        }
    }

    @Test
    fun `the greys go round both ways, the darkest at the bottom`() {
        greyShareAt(0f) shouldBe 0f
        greyShareAt(90f) shouldBe 0.5f
        greyShareAt(180f) shouldBe 1f
        greyShareAt(270f) shouldBe 0.5f
    }

    @Test
    fun `round the ring, angles go clockwise from the top`() {
        ringAngle(Offset(0f, -10f)) shouldBe 0f
        ringAngle(Offset(10f, 0f)) shouldBe 90f
        ringAngle(Offset(0f, 10f)) shouldBe 180f
        ringAngle(Offset(-10f, 0f)) shouldBe 270f
    }

    @Test
    fun `a color's code is its six hex digits, and a code typed gives its color`() {
        Palette.Orange.code() shouldBe "#F29F30"
        colorOfCode("#F29F30") shouldBe Palette.Orange
        colorOfCode("f29f30") shouldBe Palette.Orange
        colorOfCode("#F29F3") shouldBe null
        colorOfCode("#F29F300") shouldBe null
        colorOfCode("#GG9F30") shouldBe null
        colorOfCode("") shouldBe null
    }

    @Test
    fun `a code typed stays as it was typed while it gives the color, any other color shows its own`() {
        Palette.Orange.codeShown(typed = "F29F30") shouldBe "F29F30"
        Palette.Orange.codeShown(typed = "#F29F30") shouldBe "#F29F30"
        Palette.Blue.codeShown(typed = "F29F30") shouldBe "#4CC3FF"
        Palette.Blue.codeShown(typed = "#4CC3F") shouldBe "#4CC3FF"
    }
}
