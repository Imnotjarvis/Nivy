package com.nivy.wallet.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
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
import com.nivy.ui.testing.DrawnChip
import com.nivy.ui.testing.drawnChips
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

/**
 * A transaction card's chips, its category and account or a transfer's two accounts, leave as much
 * room after their text as before their first icon's drawing. Measured on the card as it's drawn.
 */
class TransactionCardChipsTest {
    private var frame: BufferedImage? = null

    @get:Rule
    val paparazzi = measuringPaparazzi { frame = it }

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

    private fun chipsOf(transaction: Transaction): List<DrawnChip> {
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
        // The chips' row starts 20 dp below the card's top and is 32 dp tall: its middle.
        return drawnChips(frame!!, rowBelowTop = 126)
    }

    private fun DrawnChip.assertBalanced() {
        // 2 dp, at the Pixel 6 Pro's 3.5 pixels a dp: the icons' drawings sit a little
        // differently in their boxes.
        assertEquals(
            "room after the text (px) to the room before the icon's",
            roomBefore.toDouble(),
            roomAfter.toDouble(),
            7.0,
        )
    }
}
