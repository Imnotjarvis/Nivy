package com.nivy.importdata.csvimport.flow

import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.data.backup.ImportResult
import com.nivy.importdata.cashew.LeftOut
import com.nivy.importdata.cashew.LeftOut.Item
import com.nivy.importdata.cashew.LeftOut.Reason
import com.nivy.importdata.csvimport.ImportSource
import com.nivy.importdata.csvimport.flow.instructions.ImportInstructions
import com.nivy.importdata.usecase.CashewImportResult
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class CashewImportPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot Cashew's steps`() {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                ImportInstructions(hasSkip = false, onSkip = {}, onUploadClick = {}, source = ImportSource.CASHEW)
            }
        }
    }

    @Test
    fun `snapshot a Cashew import, with what didn't come over`() {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                val result = ImportResult(
                    rowsFound = 1_248,
                    transactionsImported = 1_246,
                    accountsImported = 4,
                    categoriesImported = 15,
                    failedRows = persistentListOf(),
                )
                ImportResultUI(
                    result = result,
                    launchedFromOnboarding = false,
                    cashew = CashewImportResult(
                        result = result,
                        plannedPayments = 6,
                        loans = 2,
                        budgets = 3,
                        tags = 9,
                        leftOut = listOf(
                            LeftOut(
                                Reason.NoAmount,
                                listOf(Item("Free coffee", LocalDate.of(2026, 3, 8)), Item("Refund", LocalDate.of(2026, 6, 2))),
                            ),
                            LeftOut(Reason.EndsInCashew, listOf(Item("Gym", LocalDate.of(2027, 3, 1)))),
                            LeftOut(Reason.Budget, listOf(Item("Weekly groceries"), Item("Salary"))),
                            LeftOut(Reason.CategoryLimits, listOf(Item("Everything"))),
                        ),
                    ),
                    onFinish = {},
                )
            }
        }
    }
}
