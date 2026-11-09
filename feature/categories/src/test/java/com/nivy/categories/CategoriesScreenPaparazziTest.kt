package com.nivy.categories

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CategoriesScreenPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot Categories nonCompact Screen`() {
        snapshot(theme) {
            CategoriesScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Categories nonCompact Screen in minimal colors, each category its grey`() {
        snapshot(theme, minimal = true) {
            CategoriesScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Categories nonCompact Screen with search bar`() {
        snapshot(theme) {
            CategoriesScreenWithSearchBarUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Categories compact Screen`() {
        snapshot(theme) {
            CategoriesScreenCompactUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Categories compact Screen with search bar`() {
        snapshot(theme) {
            CategoriesScreenWithSearchBarCompactUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
