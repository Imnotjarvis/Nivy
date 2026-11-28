package com.nivy.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.ui.testing.measuringPaparazzi
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage

/**
 * A screen swiped down rounds its top corners on the app's radius, and at rest keeps them square.
 * Measured on a white page over black, at the Pixel 6 Pro's 3.5 pixels a dp.
 */
class SwipeBackCornersTest {
    private var frame: BufferedImage? = null

    @get:Rule
    val paparazzi = measuringPaparazzi { frame = it }

    @After
    fun tearDown() {
        AppCornerRadius.setting = null
    }

    @Test
    fun `at rest, a screen's corners are square`() {
        // given
        AppCornerRadius.setting = 32.dp

        // when
        pulled(0)

        // then
        page(x = 0f, y = 0f) shouldBe true
        page(x = Width - 1, y = 0f) shouldBe true
    }

    @Test
    fun `pulled to the point, a screen's top corners take the app's radius`() {
        // given
        AppCornerRadius.setting = 32.dp

        // when
        pulled(80)

        // then: a 32 dp corner's curve crosses its diagonal 9.4 dp in from both edges.
        val top = 80 * PixelsPerDp
        page(x = 6 * PixelsPerDp, y = top + 6 * PixelsPerDp) shouldBe false
        page(x = 13 * PixelsPerDp, y = top + 13 * PixelsPerDp) shouldBe true
        page(x = Width - 1 - 6 * PixelsPerDp, y = top + 6 * PixelsPerDp) shouldBe false
        page(x = Width - 1 - 13 * PixelsPerDp, y = top + 13 * PixelsPerDp) shouldBe true
    }

    // The finger [distanceDp] down, where letting go goes back 80 dp down, as on a screen.
    private fun pulled(distanceDp: Int) {
        val swipe = SwipeBack(80 * PixelsPerDp, canGoBack = { true }, goBack = { true }, onReachPoint = {})
        swipe.drag(distanceDp * PixelsPerDp)
        paparazzi.snapshot {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                Box(Modifier.swipeBack(swipe, dragAnywhere = true).fillMaxSize().background(Color.White))
            }
        }
    }

    // Whether the page shows at the pixel at [x], [y]: nearer white than black.
    private fun page(x: Float, y: Float): Boolean {
        val pixel = frame!!.getRGB(x.toInt(), y.toInt())
        return (pixel shr 8 and 0xFF) > 0x80
    }
}

private const val PixelsPerDp = 3.5f
private const val Width = 1440f
