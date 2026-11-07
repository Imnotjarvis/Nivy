package com.nivy.wallet.ui.component.transaction

import androidx.compose.foundation.lazy.LazyColumn
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.data.DueSection
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.ui.previewAccount
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** A transaction list's due section and history, in light and dark, in both color modes. */
@RunWith(TestParameterInjector::class)
class TransactionsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot payments to come and a paid one with a tag`() {
        val dark = theme == PaparazziTheme.Dark
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = FixedTime) {
                LazyColumn {
                    transactions(
                        baseData = AppBaseData("EUR", persistentListOf(Bank), persistentListOf()),
                        upcoming = DueSection(
                            trns = persistentListOf(salary, gym),
                            expanded = true,
                            stats = IncomeExpensePair(income = BigDecimal("1800"), expense = BigDecimal("45")),
                        ),
                        overdue = null,
                        history = persistentListOf(
                            TransactionHistoryDateDivider(
                                date = LocalDate.of(2026, 11, 5),
                                income = 0.0,
                                expenses = 32.5,
                            ),
                            TransactionHistoryEntry(transaction = dinner, tags = persistentListOf(Holiday)),
                        ),
                        emptyStateTitle = "",
                        emptyStateText = "",
                        shouldShowAccountSpecificColorInTransactions = false,
                        onPayOrGet = {},
                        setUpcomingExpanded = {},
                        setOverdueExpanded = {},
                    )
                }
            }
        }
    }

    private val salary = Income(
        id = TransactionId(UUID(0, 1)),
        title = NotBlankTrimmedString.unsafe("Salary"),
        description = null,
        category = null,
        time = Instant.parse("2026-11-27T09:00:00Z"),
        settled = false,
        metadata = NoMetadata,
        tags = emptyList(),
        value = eur(1_800.0),
        account = Bank.id,
    )

    private val gym = Expense(
        id = TransactionId(UUID(0, 2)),
        title = NotBlankTrimmedString.unsafe("Gym"),
        description = null,
        category = null,
        time = Instant.parse("2026-11-20T09:00:00Z"),
        settled = false,
        metadata = NoMetadata,
        tags = emptyList(),
        value = eur(45.0),
        account = Bank.id,
    )

    private val dinner = Expense(
        id = TransactionId(UUID(0, 3)),
        title = NotBlankTrimmedString.unsafe("Dinner out"),
        description = null,
        category = null,
        time = Instant.parse("2026-11-05T20:00:00Z"),
        settled = true,
        metadata = NoMetadata,
        tags = listOf(Holiday.id),
        value = eur(32.5),
        account = Bank.id,
    )

    private fun eur(amount: Double) = PositiveValue(PositiveDouble.unsafe(amount), AssetCode.unsafe("EUR"))

    private companion object {
        // Stored colors are the palette's own: minimal colors turn them grey where they're drawn.
        val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(0, 10))

        val Holiday = Tag(
            id = TagId(UUID(0, 20)),
            name = NotBlankTrimmedString.unsafe("Holiday"),
            description = null,
            color = ColorInt(0),
            icon = null,
            orderNum = 0.0,
            creationTimestamp = Instant.EPOCH,
        )

        val NoMetadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

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
