package com.nivy.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.nivy.base.model.Theme
import com.nivy.design.system.NivyMaterial3Theme
import com.nivy.wallet.NivyPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.Locale

/**
 * Home's quick access, open: its buttons keep the page's 24 dp from the sides, as its title and
 * the arrow do, evenly spaced, so that the arrow lines up on the right with the buttons below it.
 * Measured on the round buttons as drawn.
 */
class QuickAccessLayoutTest {
    private var frame: BufferedImage? = null
    private var circleColor = 0
    private var panelColor = 0

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        showSystemUi = false,
        useDeviceResolution = true,
        // Keeps the drawn frame for the test to measure, rather than writing or verifying it.
        snapshotHandler = object : SnapshotHandler {
            override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int) =
                object : SnapshotHandler.FrameHandler {
                    override fun handle(image: BufferedImage) {
                        frame = image
                    }

                    override fun close() = Unit
                }

            override fun close() = Unit
        },
    )

    @Test
    fun `the buttons keep the page's margin, evenly spaced, the arrow above the right ones`() {
        // given
        Locale.setDefault(Locale.US)

        // when
        paparazzi.snapshot {
            NivyMaterial3Theme(dark = false, isTrueBlack = false) {
                NivyPreview(theme = Theme.LIGHT) {
                    circleColor = MaterialTheme.colorScheme.surface.toArgb()
                    panelColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
                    MoreMenu(
                        expanded = true,
                        headerExpanded = true,
                        theme = Theme.LIGHT,
                        setExpanded = {},
                        onSwitchTheme = {},
                    )
                }
            }
        }

        // then
        val image = frame!!
        val (arrow, first, second) = circleRows(image)
        assertEquals("round buttons in the arrow's row", 1, arrow.size)
        for (row in listOf(first, second)) {
            assertEquals("buttons in a row", 4, row.size)
            val before = row.first().first
            val after = image.width - 1 - row.last().last
            assertEquals("room before the first button (px)", Margin, before.toDouble(), Tolerance)
            assertEquals("room after the last button (px)", Margin, after.toDouble(), Tolerance)
            assertEquals(
                "the arrow's right edge to the last button's (px)",
                arrow.single().last.toDouble(),
                row.last().last.toDouble(),
                Tolerance,
            )
            val gaps = row.zipWithNext { left, right -> (right.first - left.last).toDouble() }
            gaps.forEach { assertEquals("gaps between the buttons (px)", gaps.first(), it, Tolerance) }
        }
    }

    // The rows of round buttons, top to bottom, each its circles' extents from left to right: the
    // bands of pixel rows where the circles' color shows, and in each band, the columns where it
    // does. A pixel is a circle's when its color is nearer theirs than the panel's.
    private fun circleRows(image: BufferedImage): List<List<IntRange>> {
        fun circle(x: Int, y: Int): Boolean {
            val pixel = image.getRGB(x, y)
            return distance(pixel, circleColor) < distance(pixel, panelColor)
        }
        val width = 0 until image.width
        val bands = (0 until image.height).filter { y -> width.any { x -> circle(x, y) } }.runs()
        return bands.map { band -> width.filter { x -> band.any { y -> circle(x, y) } }.runs() }
    }
}

// 24 dp at the Pixel 6 Pro's 3.5 pixels a dp; and a pixel either way, for rounding.
private const val Margin = 84.0
private const val Tolerance = 1.0

// The runs of consecutive numbers in this ascending list.
private fun List<Int>.runs(): List<IntRange> {
    val runs = mutableListOf<IntRange>()
    forEach { i ->
        if (runs.lastOrNull()?.last == i - 1) runs[runs.lastIndex] = runs.last().first..i else runs += i..i
    }
    return runs
}

private fun distance(a: Int, b: Int): Int {
    val x = Color(a)
    val y = Color(b)
    val red = x.red - y.red
    val green = x.green - y.green
    val blue = x.blue - y.blue
    return red * red + green * green + blue * blue
}
