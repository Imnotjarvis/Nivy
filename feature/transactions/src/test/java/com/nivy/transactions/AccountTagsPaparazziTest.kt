package com.nivy.transactions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.navigation.NivyMaterial3Preview
import com.nivy.navigation.TransactionsScreen
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** An account's page listing its transactions with their tags: in light and dark, in both color modes. */
@RunWith(TestParameterInjector::class)
class AccountTagsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot an account's transactions with their tags`() {
        val dark = theme == PaparazziTheme.Dark
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = November7) {
                NivyMaterial3Preview(dark) {
                    UI(
                        screen = TransactionsScreen(accountId = Bank.id.value),
                        period = TimePeriod(month = Month.monthsList()[10], year = 2026),
                        baseCurrency = "EUR",
                        currency = "EUR",
                        skipAllModalVisible = false,
                        onSkipAllModalVisible = {},
                        account = Bank,
                        category = null,
                        updateAccountNameConfirmation = {},
                        enableDeletionButton = true,
                        categories = persistentListOf(Food, Transport),
                        accounts = persistentListOf(Bank),
                        balance = 2_419.5,
                        balanceBaseCurrency = null,
                        income = 0.0,
                        expenses = 80.5,
                        choosePeriodModal = null,
                        history = persistentListOf(
                            TransactionHistoryDateDivider(LocalDate.of(2026, 11, 5), income = 0.0, expenses = 32.5),
                            TransactionHistoryEntry(Dinner, tags = persistentListOf(Holiday, Family)),
                            TransactionHistoryDateDivider(LocalDate.of(2026, 11, 3), income = 0.0, expenses = 48.0),
                            TransactionHistoryEntry(Train, tags = persistentListOf(Holiday)),
                        ),
                        shouldShowAccountSpecificColorInTransactions = false,
                        onPreviousMonth = {},
                        onNextMonth = {},
                        onSetPeriod = {},
                        onEditAccount = { _, _, _ -> },
                        onEditCategory = {},
                        onDelete = {},
                        deleteModal1Visible = false,
                        onDeleteModal1Visible = {},
                        onChoosePeriodModal = {},
                    )
                }
            }
        }
    }

    private companion object {
        // Stored colors are the palette's own: minimal colors turn them grey where they're drawn.
        val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(0, 10))
        val Food = category("Restaurants", Palette.Orange, "restaurant", 1)
        val Transport = category("Transport", Palette.Purple, "transport", 2)

        val Dinner = previewTransaction(
            type = TransactionType.EXPENSE,
            account = Bank,
            amount = 32.5,
            time = Instant.parse("2026-11-05T20:00:00Z"),
            title = "Dinner out",
            category = Food.id,
            id = UUID(0, 1),
        )
        val Train = previewTransaction(
            type = TransactionType.EXPENSE,
            account = Bank,
            amount = 48.0,
            time = Instant.parse("2026-11-03T08:00:00Z"),
            title = "Train tickets",
            category = Transport.id,
            id = UUID(0, 2),
        )

        val Holiday = tag("Holiday", 20)
        val Family = tag("Family", 21)

        /** November 7, 2026, noon, in UTC. */
        val November7 = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 7, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }

        fun category(name: String, color: Color, icon: String, n: Long) = Category(
            id = CategoryId(UUID(9L, n)),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(color.toArgb()),
            icon = IconAsset.unsafe(icon),
            orderNum = n.toDouble(),
        )

        fun tag(name: String, n: Long) = Tag(
            id = TagId(UUID(0, n)),
            name = NotBlankTrimmedString.unsafe(name),
            description = null,
            color = ColorInt(0),
            icon = null,
            orderNum = 0.0,
            creationTimestamp = Instant.EPOCH,
        )
    }
}
