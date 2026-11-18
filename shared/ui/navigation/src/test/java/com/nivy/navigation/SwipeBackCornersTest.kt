package com.nivy.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.nivy.design.system.shapes.AppCornerRadius
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

    @After
    fun tearDown() {
        AppCornerRadius.setting = null
    }

    @Test
    fun `at rest, a screen's corners are square`() {
        // given
        AppCornerRadius.setting = 32.dp

        // when
        pulled(0.dp)

        // then
        page(x = 0f, y = 0f) shouldBe true
        page(x = Width - 1, y = 0f) shouldBe true
    }

    @Test
    fun `pulled to the point, a screen's top corners take the app's radius`() {
        // given
        AppCornerRadius.setting = 32.dp

        // when
        pulled(80.dp)

        // then: a 32 dp corner's curve crosses its diagonal 9.4 dp in from both edges.
        val top = 80 * PixelsPerDp
        page(x = 6 * PixelsPerDp, y = top + 6 * PixelsPerDp) shouldBe false
        page(x = 13 * PixelsPerDp, y = top + 13 * PixelsPerDp) shouldBe true
        page(x = Width - 1 - 6 * PixelsPerDp, y = top + 6 * PixelsPerDp) shouldBe false
        page(x = Width - 1 - 13 * PixelsPerDp, y = top + 13 * PixelsPerDp) shouldBe true
    }

    private fun pulled(distance: Dp) {
        paparazzi.snapshot {
            val swipe = rememberSwipeBack(canGoBack = { true }, goBack = { true })
            val px = with(LocalDensity.current) { distance.toPx() }
            remember(swipe) { swipe.drag(px) }
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
