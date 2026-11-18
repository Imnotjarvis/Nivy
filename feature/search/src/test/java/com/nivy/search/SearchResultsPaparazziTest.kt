package com.nivy.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Search's results with their totals: a salary paid, its plan's payments to come in Upcoming, and a
 * restaurant's dinners.
 */
@RunWith(TestParameterInjector::class)
class SearchResultsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
    @TestParameter
    private val results: Results,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    enum class Results(val state: SearchState) {
        Salary(SearchSample.Salary),
        Restaurant(SearchSample.Restaurant),
    }

    @Test
    fun `snapshot search results with their totals`() {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(
                theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT,
                timeProvider = SearchSample.November18,
            ) {
                Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                    // Not focused: the screenshot tests' renderer can crash starting the keyboard.
                    SearchUi(uiState = results.state, focusSearch = false, onEvent = {})
                }
            }
        }
    }
}
