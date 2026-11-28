package com.nivy.planned.list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.IntervalType
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.impl.FixedTimeProvider
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewPlannedPaymentRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/** The planned payments: a monthly expense with an end date, a monthly transfer and an income. */
@RunWith(TestParameterInjector::class)
class PlannedPaymentsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot a plan with an end date and a planned transfer`() = snapshot(theme, colors == Colors.Minimal) {
        NivyPreview(
            theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT,
            timeProvider = FixedTime,
        ) {
            UI(
                state = PlannedPaymentsScreenState(
                    currency = "EUR",
                    categories = persistentListOf(Housing, Work),
                    accounts = persistentListOf(Bank, Savings),
                    oneTimePlannedPayment = persistentListOf(),
                    oneTimeIncome = 0.0,
                    oneTimeExpenses = 0.0,
                    recurringPlannedPayment = persistentListOf(
                        previewPlannedPaymentRule(
                            account = Bank.id,
                            title = "Rent",
                            category = Housing.id,
                            amount = 750.0,
                            startDate = December1,
                            type = TransactionType.EXPENSE,
                            interval = 1 to IntervalType.MONTH,
                            endDate = Instant.parse("2027-11-30T12:00:00Z"),
                        ),
                        previewPlannedPaymentRule(
                            account = Bank.id,
                            title = "Savings",
                            category = null,
                            amount = 200.0,
                            startDate = December1,
                            type = TransactionType.TRANSFER,
                            interval = 1 to IntervalType.MONTH,
                            toAccount = Savings.id,
                        ),
                        previewPlannedPaymentRule(
                            account = Bank.id,
                            title = "Salary",
                            category = Work.id,
                            amount = 2_100.0,
                            startDate = Instant.parse("2026-11-27T12:00:00Z"),
                            type = TransactionType.INCOME,
                            interval = 1 to IntervalType.MONTH,
                        ),
                    ),
                    // A transfer counts for neither.
                    recurringIncome = 2_100.0,
                    recurringExpenses = 750.0,
                    isOneTimePaymentsExpanded = true,
                    isRecurringPaymentsExpanded = true,
                ),
            )
        }
    }

    private companion object {
        val December1: Instant = Instant.parse("2026-12-01T12:00:00Z")
        val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(0, 10))
        val Savings =
            previewAccount("Savings", color = Palette.Green, icon = "safe", currency = "EUR", id = UUID(0, 11))
        val Housing = category("Housing", Palette.Orange, 20)
        val Work = category("Work", Palette.Green, 21)

        fun category(name: String, color: Color, id: Long) = Category(
            id = CategoryId(UUID(0, id)),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(color.toArgb()),
            icon = null,
            orderNum = 0.0,
        )

        /** November 16, 2026, noon, in UTC. */
        val FixedTime = FixedTimeProvider(LocalDateTime.of(2026, 11, 16, 12, 0))
    }
}
