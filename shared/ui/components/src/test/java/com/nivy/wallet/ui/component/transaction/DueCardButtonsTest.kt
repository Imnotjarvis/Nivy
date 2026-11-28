package com.nivy.wallet.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.design.system.NivyMaterial3Theme
import com.nivy.design.system.colors.Palette
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.ui.testing.measuringPaparazzi
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
 * A payment to come's Skip and Pay buttons sit as far from its card's sides as from its bottom, in
 * line with its chips. Measured on the card as it's drawn, at a large radius.
 */
class DueCardButtonsTest {
    private var frame: BufferedImage? = null

    @get:Rule
    val paparazzi = measuringPaparazzi { frame = it }

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `a due card's buttons sit as far from its sides as from its bottom`() {
        val bank = previewAccount("Bank", color = Palette.Blue, currency = "EUR", id = UUID(0, 10))
        val gym = previewTransaction(
            type = TransactionType.EXPENSE,
            account = bank,
            amount = 45.0,
            time = Instant.parse("2026-11-20T12:00:00Z"),
            settled = false,
            title = "Gym",
        )
        AppCornerRadius.setting = 40.dp
        try {
            paparazzi.snapshot {
                NivyMaterial3Theme(dark = false, isTrueBlack = false) {
                    NivyPreview(theme = Theme.LIGHT) {
                        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            TransactionCard(
                                baseData = AppBaseData("EUR", persistentListOf(bank), persistentListOf()),
                                transaction = gym,
                                shouldShowAccountSpecificColorInTransactions = false,
                                onPayOrGet = {},
                                onClick = {},
                            )
                        }
                    }
                }
            }
        } finally {
            AppCornerRadius.setting = null
        }

        val insets = ButtonInsets.of(frame!!)
        assertEquals("left inset (px) to the bottom's", insets.bottom.toDouble(), insets.left.toDouble(), 2.0)
        assertEquals("right inset (px) to the bottom's", insets.bottom.toDouble(), insets.right.toDouble(), 2.0)
    }

    /** How far, in pixels, the card's buttons sit from its left, right and bottom edges. */
    private data class ButtonInsets(val left: Int, val right: Int, val bottom: Int) {
        companion object {
            fun of(image: BufferedImage): ButtonInsets {
                val page = image.getRGB(2, 2)
                val center = image.width / 2
                // Down the card's middle, between the buttons: the card's top and bottom edges; its
                // sides across its middle, clear of its rounded corners.
                val top = (0 until image.height).first { !near(image.getRGB(center, it), page) }
                val card = image.getRGB(center, top + 4)
                val cardBottom = (top until image.height).first { near(image.getRGB(center, it), page) } - 1
                val across = (top + cardBottom) / 2
                val cardLeft = (0 until center).first { near(image.getRGB(it, across), card) }
                val cardRight = (image.width - 1 downTo center).first { near(image.getRGB(it, across), card) }
                // Up from the card's bottom, through Skip past its rounded corner: Skip's bottom
                // and top edges, then across its middle, where its sides are straight.
                val x = cardLeft + (cardRight - cardLeft) / 5
                val buttonBottom = (cardBottom downTo top).first { !near(image.getRGB(x, it), card) }
                val buttonTop = (buttonBottom downTo top).first { near(image.getRGB(x, it), card) }
                val middle = (buttonTop + buttonBottom) / 2
                val skipLeft = (cardLeft until center).first { !near(image.getRGB(it, middle), card) }
                val payRight = (cardRight downTo center).first { !near(image.getRGB(it, middle), card) }
                return ButtonInsets(
                    left = skipLeft - cardLeft,
                    right = cardRight - payRight,
                    bottom = cardBottom - buttonBottom,
                )
            }

            // The same color, give or take anti-aliasing.
            private fun near(a: Int, b: Int): Boolean =
                listOf(16, 8, 0).all { shift -> abs((a shr shift and 0xFF) - (b shr shift and 0xFF)) <= 3 }
        }
    }
}
