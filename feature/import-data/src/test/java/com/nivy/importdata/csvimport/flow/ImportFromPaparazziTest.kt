package com.nivy.importdata.csvimport.flow

import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class ImportFromPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot what to import from`() {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                ImportFrom(launchedFromOnboarding = false)
            }
        }
    }
}
