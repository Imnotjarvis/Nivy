package com.nivy.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.home.usecase.Glance
import com.nivy.home.usecase.NextPayment
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(TestParameterInjector::class)
class HomePaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

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

    // The header's glance, a fact a row: overdue, spent today, then the next payment as an
    // expense, an income, and one with a name too long to show whole.
    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun `snapshot the glance, a fact a row`(@TestParameter colors: Colors) {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    GlanceRows.forEach { (glance, fact) ->
                        HomeHeader(
                            expanded = true,
                            glance = glance,
                            period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                            onShowMonthModal = {},
                            onSelectNextMonth = {},
                            onSelectPreviousMonth = {},
                            firstGlanceFact = fact,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val Today = Glance(
            overdue = 2,
            spentToday = BigDecimal("12.40"),
            baseCurrency = "EUR",
            nextPayment = NextPayment(
                name = "Rent",
                type = TransactionType.EXPENSE,
                amount = 850.0,
                currency = "EUR",
                daysLeft = 3,
            ),
        )
        val GlanceRows = listOf(
            Today to GlanceFact.Overdue,
            Today to GlanceFact.SpentToday,
            Today to GlanceFact.NextPayment,
            Today.copy(
                nextPayment = NextPayment(
                    name = "Salary",
                    type = TransactionType.INCOME,
                    amount = 2_400.0,
                    currency = "EUR",
                    daysLeft = 1,
                ),
            ) to GlanceFact.NextPayment,
            Today.copy(
                nextPayment = NextPayment(
                    name = "Car insurance and road tax",
                    type = TransactionType.EXPENSE,
                    amount = 1_250.0,
                    currency = "EUR",
                    daysLeft = 0,
                ),
            ) to GlanceFact.NextPayment,
        )
    }
}
