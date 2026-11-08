package com.nivy.loans

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.LoanRecordType
import com.nivy.base.model.Theme
import com.nivy.base.time.TimeProvider
import com.nivy.design.system.colors.Palette
import com.nivy.loans.loandetails.LoanRecordModal
import com.nivy.loans.loandetails.LoanRecordModalData
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewLoanRecord
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** The loan dialogs, with an account picked among three: in light and dark, in both color modes. */
@RunWith(TestParameterInjector::class)
class LoanDialogsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    private val bank = previewAccount("Bank", color = Palette.Blue, currency = "EUR")
    private val accounts = listOf(
        previewAccount("Cash", color = Palette.Green, currency = "EUR"),
        bank,
        previewAccount("Savings", color = Palette.Orange, currency = "EUR"),
    )
    private val record = previewLoanRecord(amount = 50.0, dateTime = FixedTime.utcNow(), type = LoanRecordType.DECREASE)

    @Test
    fun `snapshot the new loan dialog`() = shot {
        LoanModal(
            modal = LoanModalData(loan = null, baseCurrency = "EUR", selectedAccount = bank, autoFocusKeyboard = false),
            dateTime = FixedTime.utcNow(),
            onSetDate = {},
            onSetTime = {},
            onCreateLoan = {},
            onEditLoan = { _, _ -> },
            accounts = accounts,
            dismiss = {},
        )
    }

    // A record being edited: a new one opens the keypad over the dialog.
    @Test
    fun `snapshot the loan record dialog`() = shot {
        LoanRecordModal(
            modal = LoanRecordModalData(
                loanRecord = record,
                baseCurrency = "EUR",
                selectedAccount = bank,
            ),
            dateTime = FixedTime.utcNow(),
            onSetDate = {},
            onSetTime = {},
            onCreate = {},
            onEdit = {},
            onDelete = {},
            dismiss = {},
            accounts = accounts,
        )
    }

    // The dialogs show the loan's date: on a stopped clock, one that stays the same.
    private fun shot(content: @Composable BoxWithConstraintsScope.() -> Unit) =
        snapshot(theme, minimal = colors == Colors.Minimal) {
            val dark = theme == PaparazziTheme.Dark
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = FixedTime) {
                content()
            }
        }
}

/** November 7, 2026, noon, in UTC. */
private object FixedTime : TimeProvider {
    private val now = LocalDateTime.of(2026, 11, 7, 12, 0)
    override fun getZoneId(): ZoneId = ZoneId.of("UTC")
    override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
    override fun localNow(): LocalDateTime = now
    override fun localDateNow(): LocalDate = now.toLocalDate()
    override fun localTimeNow(): LocalTime = now.toLocalTime()
}
