package com.nivy.planned.list

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.nivy.base.model.IntervalType
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.NivyMaterial3Theme
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.drawnChips
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewPlannedPaymentRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.Locale
import java.util.UUID

/**
 * A planned payment's category and account chips leave as much room after their text as before
 * their icon's drawing. Measured on the card as it's drawn.
 */
class PlannedPaymentCardChipsTest {
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

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `a planned payment's chips leave as much room after their text as before their icon`() {
        val bank = previewAccount("Main Bank", color = Palette.Blue, currency = "EUR", id = UUID(0, 10))
        val housing = Category(
            id = CategoryId(UUID(0, 20)),
            name = NotBlankTrimmedString.unsafe("Housing"),
            color = ColorInt(Palette.Orange.toArgb()),
            icon = IconAsset.unsafe("house"),
            orderNum = 0.0,
        )
        val rent = previewPlannedPaymentRule(
            account = bank.id,
            title = "Rent",
            category = housing.id,
            amount = 750.0,
            startDate = Instant.parse("2026-12-01T12:00:00Z"),
            type = TransactionType.EXPENSE,
            interval = 1 to IntervalType.MONTH,
        )

        paparazzi.snapshot {
            NivyMaterial3Theme(dark = false, isTrueBlack = false) {
                NivyPreview(theme = Theme.LIGHT) {
                    LazyColumn(Modifier.background(MaterialTheme.colorScheme.background)) {
                        item {
                            PlannedPaymentCard(
                                baseCurrency = "EUR",
                                categories = persistentListOf(housing),
                                accounts = persistentListOf(bank),
                                plannedPayment = rent,
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }
        // The chips' row starts 20 dp below the card's top and is 32 dp tall: its middle.
        val chips = drawnChips(frame!!, rowBelowTop = 126)

        assertEquals("chips", 2, chips.size)
        chips.forEach { chip ->
            // 2 dp, at the Pixel 6 Pro's 3.5 pixels a dp: the icons' drawings sit a little
            // differently in their boxes.
            assertEquals(
                "room after the text (px) to the room before the icon's",
                chip.roomBefore.toDouble(),
                chip.roomAfter.toDouble(),
                7.0,
            )
        }
    }
}
