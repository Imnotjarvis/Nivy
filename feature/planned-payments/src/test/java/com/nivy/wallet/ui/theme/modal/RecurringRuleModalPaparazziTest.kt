package com.nivy.wallet.ui.theme.modal

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.IntervalType
import com.nivy.base.model.Theme
import com.nivy.base.time.impl.FixedTimeProvider
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** The schedule dialog of a monthly plan that ends: Ends on a date, a year on. */
@RunWith(TestParameterInjector::class)
class RecurringRuleModalPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the schedule dialog with an end date`() = snapshot(theme, minimal = colors == Colors.Minimal) {
        NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT, timeProvider = FixedTime) {
            RecurringRuleModal(
                modal = RecurringRuleModalData(
                    initialStartDate = LocalDateTime.of(2026, 12, 1, 12, 0),
                    initialIntervalN = 1,
                    initialIntervalType = IntervalType.MONTH,
                    initialEndDate = LocalDateTime.of(2027, 11, 30, 12, 0),
                ),
                dismiss = {},
                onRuleChanged = {},
            )
        }
    }

    private companion object {
        /** November 16, 2026, noon, in UTC. */
        val FixedTime = FixedTimeProvider(LocalDateTime.of(2026, 11, 16, 12, 0))
    }
}
