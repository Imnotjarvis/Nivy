package com.nivy.exchangerates.modal

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith

/** The dialog that adds a rate by hand, as it opens: nothing typed yet. */
@RunWith(TestParameterInjector::class)
class AddRateDialogPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the add rate dialog as it opens`() = snapshot(theme, minimal = colors == Colors.Minimal) {
        NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
            AddRateModal(visible = true, baseCurrency = "EUR", dismiss = {}, onAdd = {})
        }
    }
}
