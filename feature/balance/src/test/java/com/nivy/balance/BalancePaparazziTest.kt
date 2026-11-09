package com.nivy.balance

import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith

/** The net worth screen with the month in detail, scrolled part by part. */
@RunWith(TestParameterInjector::class)
class BalancePaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
    @TestParameter
    private val part: Part,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    /** How far down, in dp, the screen is scrolled for each part: a screenful less a little each. */
    enum class Part(val scrolled: Int) {
        Top(0),
        Second(780),
        Third(1_560),
        Fourth(2_340),
        Fifth(3_120),
        // As far as it goes.
        Bottom(100_000),
    }

    @Test
    fun `snapshot the net worth screen with the month in detail`() {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(
                theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT,
                timeProvider = BalanceSample.November9,
            ) {
                val scrolled = with(LocalDensity.current) { part.scrolled.dp.roundToPx() }
                UI(state = BalanceSample.State, scrollState = rememberScrollState(scrolled))
            }
        }
    }
}
