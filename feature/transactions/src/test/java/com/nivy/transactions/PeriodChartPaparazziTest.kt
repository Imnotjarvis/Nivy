package com.nivy.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.impl.FixedTimeProvider
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
import java.util.UUID

/**
 * An account's and a category's chart of the period, and a page with it folded under the period's
 * switcher: in light and dark, in both color modes.
 */
@RunWith(TestParameterInjector::class)
class PeriodChartPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot an account's income and expenses added up to today`() = panel(AccountChart)

    @Test
    fun `snapshot a category's spending added up over the month before's`() = panel(CategoryChart)

    @Test
    fun `snapshot a page with its chart folded under the period's switcher`() {
        val dark = theme == PaparazziTheme.Dark
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = Today24) {
                NivyMaterial3Preview(dark) {
                    UI(
                        screen = TransactionsScreen(accountId = Bank.id.value),
                        period = November,
                        baseCurrency = "EUR",
                        currency = "EUR",
                        skipAllModalVisible = false,
                        onSkipAllModalVisible = {},
                        account = Bank,
                        category = null,
                        updateAccountNameConfirmation = {},
                        enableDeletionButton = true,
                        categories = persistentListOf(),
                        accounts = persistentListOf(Bank),
                        balance = 2_419.5,
                        balanceBaseCurrency = null,
                        income = 1_910.0,
                        expenses = 1_255.59,
                        choosePeriodModal = null,
                        history = persistentListOf(
                            TransactionHistoryDateDivider(LocalDate.of(2026, 11, 24), income = 0.0, expenses = 3.8),
                            TransactionHistoryEntry(Coffee),
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
                        periodChart = AccountChart,
                        periodChartExpanded = false,
                        onChoosePeriodModal = {},
                    )
                }
            }
        }
    }

    private fun panel(chart: PeriodChart) {
        val dark = theme == PaparazziTheme.Dark
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = Today24) {
                NivyMaterial3Preview(dark) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 16.dp)
                    ) {
                        PeriodChartPanel(
                            chart = chart,
                            period = November,
                            currency = "EUR",
                            expanded = true,
                            onExpandedChange = {},
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val November = TimePeriod(month = Month.monthsList()[10], year = 2026)
        val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(0, 10))
        val Coffee = previewTransaction(
            type = TransactionType.EXPENSE,
            account = Bank,
            amount = 3.8,
            time = Instant.parse("2026-11-24T08:00:00Z"),
            title = "Coffee",
            id = UUID(0, 1),
        )

        // Bank in November, to the 24th: the rent on the 1st, the salary on the 10th, a refund on the 18th.
        val AccountChart = accountChart(
            days = nov(1)..nov(30),
            today = nov(24),
            flows = listOf(
                Flow(nov(1), expense = 550.0), Flow(nov(2), expense = 46.3), Flow(nov(3), expense = 48.0),
                Flow(nov(5), expense = 32.5), Flow(nov(6), expense = 38.9), Flow(nov(8), expense = 35.0),
                Flow(nov(9), expense = 12.5), Flow(nov(10), income = 1_850.0), Flow(nov(11), expense = 52.1),
                Flow(nov(12), expense = 18.4), Flow(nov(14), expense = 120.5), Flow(nov(15), expense = 64.0),
                Flow(nov(16), expense = 41.2), Flow(nov(18), income = 60.0), Flow(nov(19), expense = 29.99),
                Flow(nov(20), expense = 57.8), Flow(nov(21), expense = 24.0), Flow(nov(22), expense = 47.5),
                Flow(nov(23), expense = 33.1), Flow(nov(24), expense = 3.8),
            ),
        )!!

        // Restaurants in November, to the 24th, over October.
        val CategoryChart = categoryChart(
            days = nov(1)..nov(30),
            today = nov(24),
            flows = listOf(18.0 to 2, 32.5 to 5, 12.5 to 9, 22.4 to 13, 64.0 to 15, 15.8 to 19, 47.5 to 22, 3.8 to 24)
                .map { (amount, day) -> Flow(nov(day), expense = amount) },
            before = (oct(1)..oct(31)) to
                listOf(25.0 to 3, 41.5 to 8, 18.2 to 11, 55.0 to 17, 12.0 to 20, 38.4 to 24, 22.0 to 26, 41.8 to 31)
                    .map { (amount, day) -> Flow(oct(day), expense = amount) },
        )!!

        fun nov(day: Int): LocalDate = LocalDate.of(2026, 11, day)
        fun oct(day: Int): LocalDate = LocalDate.of(2026, 10, day)

        /** November 24, 2026, noon, in UTC. */
        val Today24 = FixedTimeProvider(LocalDateTime.of(2026, 11, 24, 12, 0))
    }
}
