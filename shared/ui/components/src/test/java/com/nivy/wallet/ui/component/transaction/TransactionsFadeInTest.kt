package com.nivy.wallet.ui.component.transaction

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.design.system.NivyMaterial3Theme
import com.nivy.design.system.colors.Palette
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.data.DueSection
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/**
 * Home's list fades its rows in as they come. Fading in must leave the list as it is laid out:
 * a section's or a day's header keeps the room above and below it, so the list fading in draws
 * exactly what the plain list draws, with full cards or compact rows.
 */
class TransactionsFadeInTest {
    private val frames = mutableMapOf<String, BufferedImage>()

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        // Keeps each rendered frame for the test to compare, rather than writing or verifying it.
        snapshotHandler = object : SnapshotHandler {
            override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int) =
                object : SnapshotHandler.FrameHandler {
                    override fun handle(image: BufferedImage) {
                        frames[snapshot.name.orEmpty()] = image
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
    fun `the list fading in is drawn as the plain list is`() {
        for (compact in listOf(false, true)) {
            paparazzi.snapshot(name = "plain") { HomeList(fadeIn = false, compact = compact) }
            paparazzi.snapshot(name = "fading in") { HomeList(fadeIn = true, compact = compact) }
            assertSameImage(frames.getValue("plain"), frames.getValue("fading in"), "compact = $compact")
        }
    }

    private fun assertSameImage(plain: BufferedImage, fadingIn: BufferedImage, case: String) {
        assertEquals("$case: size", plain.width to plain.height, fadingIn.width to fadingIn.height)
        val rows = (0 until plain.height).filter { y ->
            (0 until plain.width).any { x -> plain.getRGB(x, y) != fadingIn.getRGB(x, y) }
        }
        assertTrue(
            "$case: fading in drew ${rows.size} of ${plain.height} rows otherwise, from row ${rows.firstOrNull()}",
            rows.isEmpty(),
        )
    }

    // As on Home: the line under the cash flow, then payments to come and two days of history.
    @Composable
    private fun HomeList(fadeIn: Boolean, compact: Boolean) {
        NivyMaterial3Theme(dark = false, isTrueBlack = false) {
            NivyPreview(theme = Theme.LIGHT, timeProvider = FixedTime) {
                LazyColumn {
                    item { TransactionsDividerLine() }
                    transactions(
                        baseData = AppBaseData("EUR", persistentListOf(Bank), persistentListOf()),
                        upcoming = DueSection(
                            trns = persistentListOf(Gym),
                            expanded = true,
                            stats = IncomeExpensePair(income = BigDecimal.ZERO, expense = BigDecimal("35")),
                        ),
                        overdue = null,
                        history = persistentListOf(
                            TransactionHistoryDateDivider(LocalDate.of(2026, 11, 6), income = 0.0, expenses = 12.4),
                            TransactionHistoryEntry(Lunch),
                            TransactionHistoryEntry(Bus),
                            TransactionHistoryDateDivider(LocalDate.of(2026, 11, 5), income = 0.0, expenses = 42.5),
                            TransactionHistoryEntry(Dinner),
                        ),
                        emptyStateTitle = "",
                        emptyStateText = "",
                        shouldShowAccountSpecificColorInTransactions = false,
                        onPayOrGet = {},
                        setUpcomingExpanded = {},
                        setOverdueExpanded = {},
                        compact = compact,
                        fadeIn = fadeIn,
                    )
                }
            }
        }
    }

    private companion object {
        val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(0, 10))

        private fun expense(n: Long, title: String, amount: Double, at: String, settled: Boolean = true) =
            previewTransaction(
                type = TransactionType.EXPENSE,
                account = Bank,
                amount = amount,
                time = Instant.parse(at),
                settled = settled,
                title = title,
                id = UUID(0, n),
            )

        val Gym = expense(1, "Gym", 35.0, "2026-11-10T09:00:00Z", settled = false)
        val Lunch = expense(2, "Lunch", 8.9, "2026-11-06T12:00:00Z")
        val Bus = expense(3, "Bus ticket", 3.5, "2026-11-06T08:00:00Z")
        val Dinner = expense(4, "Dinner out", 42.5, "2026-11-05T20:00:00Z")

        /** November 7, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 7, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
    }
}
