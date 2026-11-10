package com.nivy.piechart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** The Unspecified slice selected, like a category's: the other slices fade, and its card comes first. */
@RunWith(TestParameterInjector::class)
class PieChartUnspecifiedPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun `snapshot the Unspecified slice selected`() {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT, timeProvider = November9) {
                UI(
                    state = PieChartStatisticState(
                        transactionType = TransactionType.EXPENSE,
                        period = TimePeriod(month = Month.fromMonthValue(11), year = 2026),
                        baseCurrency = "EUR",
                        totalAmount = 1_250.0,
                        categoryAmounts = persistentListOf(
                            CategoryAmount(category = null, amount = 230.0, isCategoryUnspecified = true),
                            CategoryAmount(category = Groceries, amount = 520.0),
                            CategoryAmount(category = EatingOut, amount = 380.0),
                            CategoryAmount(category = Bills, amount = 120.0),
                        ),
                        selectedCategory = SelectedCategory(category = null),
                        accountIdFilterList = persistentListOf(),
                        showCloseButtonOnly = false,
                        transactions = persistentListOf(),
                        choosePeriodModal = null,
                    )
                )
            }
        }
    }

    private companion object {
        val Groceries = category("Groceries", Palette.Orange, "groceries", 1)
        val EatingOut = category("Eating out", Palette.Red, "restaurant", 2)
        val Bills = category("Bills", Palette.Green2, "bills", 3)

        fun category(name: String, color: Color, icon: String, n: Long) = Category(
            id = CategoryId(UUID(9L, n)),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(color.toArgb()),
            icon = IconAsset.unsafe(icon),
            orderNum = n.toDouble(),
        )

        /** November 9, 2026, noon, in UTC. */
        val November9 = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 9, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
    }
}
