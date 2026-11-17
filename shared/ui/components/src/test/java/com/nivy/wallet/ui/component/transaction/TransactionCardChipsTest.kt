package com.nivy.wallet.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Transaction
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.NivyMaterial3Theme
import com.nivy.design.system.colors.Palette
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * A transaction card's chips, its category and account or a transfer's two accounts, leave as much
 * room after their text as before their first icon's drawing. Measured on the card as it's drawn.
 */
class TransactionCardChipsTest {
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

    private val bank = previewAccount("Main Bank", color = Palette.Blue, currency = "EUR", id = UUID(0, 10))
    private val savings = previewAccount("Savings", color = Palette.Purple1, currency = "EUR", id = UUID(0, 11))
    private val fitness = Category(
        id = CategoryId(UUID(0, 20)),
        name = NotBlankTrimmedString.unsafe("Fitness"),
        color = ColorInt(Palette.Orange.toArgb()),
        icon = IconAsset.unsafe("fitness"),
        orderNum = 0.0,
    )

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `an expense's category and account chips leave as much room after their text as before`() {
        val gym = previewTransaction(
            type = TransactionType.EXPENSE,
            account = bank,
            amount = 45.0,
            time = Instant.parse("2026-11-20T12:00:00Z"),
            title = "Gym",
            category = fitness.id,
        )

        val chips = chipsOf(gym)

        assertEquals("chips", 2, chips.size)
        chips.forEach { it.assertBalanced() }
    }

    @Test
    fun `a transfer's accounts chip leaves as much room after its text as before`() {
        val toSavings = previewTransaction(
            type = TransactionType.TRANSFER,
            account = bank,
            amount = 200.0,
            time = Instant.parse("2026-11-20T12:00:00Z"),
            title = "To savings",
            toAccount = savings,
        )

        val chips = chipsOf(toSavings)

        assertEquals("chips", 1, chips.size)
        chips.single().assertBalanced()
    }

    private fun chipsOf(transaction: Transaction): List<Chip> {
        paparazzi.snapshot {
            NivyMaterial3Theme(dark = false, isTrueBlack = false) {
                NivyPreview(theme = Theme.LIGHT) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        TransactionCard(
                            baseData = AppBaseData(
                                baseCurrency = "EUR",
                                accounts = persistentListOf(bank, savings),
                                categories = persistentListOf(fitness),
                            ),
                            transaction = transaction,
                            shouldShowAccountSpecificColorInTransactions = false,
                            onPayOrGet = {},
                            onClick = {},
                        )
                    }
                }
            }
        }
        return Chip.allIn(frame!!)
    }

    /**
     * A chip as drawn: its edges, in pixels, along the row through its middle, and where its icons
     * and text start and end.
     */
    private data class Chip(val left: Int, val right: Int, val drawingStart: Int, val drawingEnd: Int) {
        fun assertBalanced() {
            val before = drawingStart - left
            val after = right - drawingEnd
            assertEquals(
                "room after the text (px) to the room before the icon's",
                before.toDouble(),
                after.toDouble(),
                Tolerance,
            )
        }

        companion object {
            // 2 dp, at the Pixel 6 Pro's 3.5 pixels a dp: the icons' drawings sit a little
            // differently in their boxes.
            private const val Tolerance = 7.0

            fun allIn(image: BufferedImage): List<Chip> {
                val page = image.getRGB(2, 2)
                // Down the card's middle, clear of its rounded corners: its top. Across the chips'
                // row: the card's sides, then its first chip, 20 dp in from its side.
                val center = image.width / 2
                val top = (0 until image.height).first { !near(image.getRGB(center, it), page) }
                val card = image.getRGB(center, top + 4)
                val row = top + ChipsBelowTop
                val cardLeft = (0 until image.width).first { near(image.getRGB(it, row), card) }
                val cardRight = (image.width - 1 downTo 0).first { near(image.getRGB(it, row), card) }
                val chipX = (cardLeft..cardRight).first { !near(image.getRGB(it, row), card) }
                val chipTop = (top until image.height).first { !near(image.getRGB(chipX + 6, it), card) }
                val chipBottom = (chipTop until image.height).first { near(image.getRGB(chipX + 6, it), card) } - 1
                val middle = (chipTop + chipBottom) / 2
                // The runs of chip along the middle row, joined across the pixels of its text and
                // icons that happen to match the card's color.
                val runs = mutableListOf<IntRange>()
                var x = cardLeft
                while (x <= cardRight) {
                    if (near(image.getRGB(x, middle), card)) {
                        x++
                        continue
                    }
                    val start = x
                    var end = x
                    var gap = 0
                    while (x <= cardRight && gap < MaxGap) {
                        if (near(image.getRGB(x, middle), card)) {
                            gap++
                        } else {
                            gap = 0
                            end = x
                        }
                        x++
                    }
                    runs += start..end
                }
                val inside = (chipTop + (chipBottom - chipTop) / 5)..(chipBottom - (chipBottom - chipTop) / 5)
                return runs.filter { it.last - it.first > MinChipWidth }.map { run ->
                    val fill = image.getRGB(run.first + 4, middle)
                    val drawn = run.filter { col -> inside.any { row -> contrasts(image.getRGB(col, row), fill) } }
                    Chip(left = run.first, right = run.last, drawingStart = drawn.first(), drawingEnd = drawn.last())
                }
            }

            // The chips' row starts 20 dp below the card's top; this is 4 dp into it.
            private const val ChipsBelowTop = 84
            private const val MaxGap = 20
            private const val MinChipWidth = 100

            // The same color, give or take anti-aliasing.
            private fun near(a: Int, b: Int): Boolean =
                listOf(16, 8, 0).all { shift -> abs((a shr shift and 0xFF) - (b shr shift and 0xFF)) <= 3 }

            // An icon's or a letter's pixel on the chip's fill.
            private fun contrasts(a: Int, b: Int): Boolean = abs(luminance(a) - luminance(b)) > 90

            private fun luminance(c: Int): Double =
                0.2126 * (c shr 16 and 0xFF) + 0.7152 * (c shr 8 and 0xFF) + 0.0722 * (c and 0xFF)
        }
    }
}
