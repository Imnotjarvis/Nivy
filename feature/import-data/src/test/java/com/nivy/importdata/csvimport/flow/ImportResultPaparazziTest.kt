package com.nivy.importdata.csvimport.flow

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.data.backup.CSVRow
import com.nivy.data.backup.ImportResult
import com.nivy.design.system.colors.ColorMode
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class ImportResultPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot an import with rows it couldn't read`() {
        ColorMode.minimal = colors == Colors.Minimal
        try {
            snapshot(theme) {
                NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                    ImportResultUI(
                        result = ImportResult(
                            rowsFound = 48,
                            transactionsImported = 41,
                            accountsImported = 3,
                            categoriesImported = 7,
                            failedRows = persistentListOf(
                                CSVRow(6, listOf("2026-11-05T18:00:00", "Nothing", "", "Bank", "0.00", "EUR")),
                                CSVRow(7, listOf("2026-11-06T09:00:00", "Card payment", "", "Bank", "200.00")),
                                CSVRow(12, listOf("", "Coffee", "Food", "Cash", "3.20", "EUR", "EXPENSE")),
                                CSVRow(19, listOf("2026-11-09T08:15:00", "Refund", "Shopping", "Card", "-", "EUR")),
                                CSVRow(23, listOf("2026-11-11T19:40:00", "Dinner", "Food", "", "38.00", "EUR")),
                                CSVRow(31, listOf("2026-11-14T10:00:00", "Gift", "Gifts", "Cash", "0.00", "EUR")),
                                CSVRow(40, listOf("2026-11-20T07:30:00", "Train", "Travel", "Card", "abc", "EUR")),
                            ),
                        ),
                        launchedFromOnboarding = false,
                        onFinish = {},
                    )
                }
            }
        } finally {
            ColorMode.minimal = false
        }
    }
}
