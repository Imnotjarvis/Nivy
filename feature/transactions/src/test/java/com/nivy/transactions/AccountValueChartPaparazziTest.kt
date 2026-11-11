package com.nivy.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.data.model.Month
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

/** An investment account's value chart: in light and dark, in both color modes. */
@RunWith(TestParameterInjector::class)
class AccountValueChartPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot an account's value over the money invested`() {
        snapshot(theme, minimal = colors == Colors.Minimal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                ValueChart(points = Points)
            }
        }
    }

    private companion object {
        // June to November: money put in three times, and a value that dips once on the way up.
        val Points = persistentListOf(
            ValuePoint(Month.monthsList()[5], value = 5_000.0, invested = 5_000.0),
            ValuePoint(Month.monthsList()[6], value = 5_240.0, invested = 5_000.0),
            ValuePoint(Month.monthsList()[7], value = 6_180.0, invested = 6_000.0),
            ValuePoint(Month.monthsList()[8], value = 5_910.0, invested = 6_000.0),
            ValuePoint(Month.monthsList()[9], value = 7_350.0, invested = 7_000.0),
            ValuePoint(Month.monthsList()[10], value = 7_820.0, invested = 7_000.0),
        )
    }
}
