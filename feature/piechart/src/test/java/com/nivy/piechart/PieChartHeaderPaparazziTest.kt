package com.nivy.piechart

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PieChartHeaderPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    // Collapsed, with a total too wide to show whole: shortened, its number before its code.
    @Test
    fun `snapshot the collapsed header with a shortened total`() {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                Header(
                    transactionType = TransactionType.EXPENSE,
                    period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                    percentExpanded = 0f,
                    currency = "EUR",
                    amount = 1_234_567.89,
                    onShowMonthModal = {},
                    onSelectNextMonth = {},
                    onSelectPreviousMonth = {},
                    onSelectPeriod = {},
                    onClose = {},
                    onAdd = {},
                )
            }
        }
    }
}
