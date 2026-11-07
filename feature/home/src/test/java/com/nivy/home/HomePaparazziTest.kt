package com.nivy.home

import androidx.compose.animation.ExperimentalAnimationApi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.base.model.Theme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class HomePaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot Home Screen`() {
        snapshot(theme) {
            HomeUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun `snapshot the month's amounts with the income hidden`() {
        snapshot(theme) {
            NivyPreview(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                CashFlowInfo(
                    currency = "EUR",
                    balance = 2_400.0,
                    monthlyIncome = 1_000.0,
                    monthlyExpenses = 400.0,
                    hideBalance = false,
                    hideIncome = true,
                    onHiddenIncomeClick = {},
                    onBalanceClick = {},
                    onHiddenBalanceClick = {},
                )
            }
        }
    }
}
