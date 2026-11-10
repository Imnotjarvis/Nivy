package com.nivy.wallet.ui.theme.modal

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith

/** The icon picker's first screen, the loan dialog's own icon chosen. */
@RunWith(TestParameterInjector::class)
class IconPickerPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the icon picker`() = snapshot(theme, minimal = colors == Colors.Minimal) {
        NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
            ChooseIconModal(
                visible = true,
                initialIcon = "loan",
                color = Palette.Blue,
                dismiss = {},
                onIconChosen = {},
            )
        }
    }
}
