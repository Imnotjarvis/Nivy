package com.nivy.balance.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.balance.BalanceSample
import com.nivy.base.model.Theme
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The month day by day without an outlook, before there's history enough for one: the balance,
 * what's planned, and the lowest point the planned payments take it to. In light and dark, in both
 * color modes.
 */
@RunWith(TestParameterInjector::class)
class DayByDayPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the lowest point without an outlook`() {
        val dark = theme == PaparazziTheme.Dark
        val detail = checkNotNull(BalanceSample.State.detail).copy(outlook = null)
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = BalanceSample.November9) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top = 24.dp)) {
                    DayByDayPanel(detail, currency = "EUR")
                }
            }
        }
    }
}
