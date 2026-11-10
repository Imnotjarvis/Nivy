package com.nivy.loans.loandetails

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.LoanType
import com.nivy.base.model.Theme
import com.nivy.design.system.colors.Palette
import com.nivy.loans.LoanProgress
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewLoan
import org.junit.Test
import org.junit.runner.RunWith

/** A loan's page header, part paid back: in light and dark, in both color modes. */
@RunWith(TestParameterInjector::class)
class LoanDetailsHeaderPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot a loan part paid back`() = snapshot(theme, minimal = colors == Colors.Minimal) {
        val dark = theme == PaparazziTheme.Dark
        NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT) {
            val loan = previewLoan("Car", amount = 1_300.0, color = Palette.Blue, type = LoanType.BORROW)
            Header(
                loan = loan,
                baseCurrency = "EUR",
                progress = LoanProgress(total = 1_300.0, paid = 200.0, interestPaid = 50.0),
                itemColor = Palette.Blue,
                onAmountClick = {},
                onEditLoan = {},
                onDeleteLoan = {},
                selectedLoanAccount = previewAccount("Bank", color = Palette.Green, currency = "EUR"),
                onAddRecord = {},
            )
        }
    }
}
