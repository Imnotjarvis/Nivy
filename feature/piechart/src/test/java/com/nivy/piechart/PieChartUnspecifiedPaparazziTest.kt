package com.nivy.piechart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Color
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.impl.FixedTimeProvider
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.ui.previewCategory
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
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

        fun category(name: String, color: Color, icon: String, n: Long) =
            previewCategory(name, color, icon, id = UUID(9L, n), orderNum = n.toDouble())

        /** November 9, 2026, noon, in UTC. */
        val November9 = FixedTimeProvider(LocalDateTime.of(2026, 11, 9, 12, 0))
    }
}
