package com.nivy.wallet.ui.theme.wallet

import androidx.compose.runtime.withCompositionLocals
import com.nivy.base.time.TimeProvider
import com.nivy.ui.testing.runComposeTest
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.LocalPeriodState
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.LocalDate

class BackToThisMonthTest {
    private val clock = mockk<TimeProvider> { every { localDateNow() } returns LocalDate.of(2026, 11, 15) }
    private val periodState = PeriodState(clock)

    @Test
    fun `another period goes back to this month`() {
        var selected: TimePeriod? = null

        runComposeTest(present = { action(October) { selected = it } }) {
            this?.invoke()
        }

        selected shouldBe November
    }

    @Test
    fun `this month has no way back, so no x`() {
        runComposeTest(present = { action(November) {} }) {
            this shouldBe null
        }
    }

    @androidx.compose.runtime.Composable
    private fun action(period: TimePeriod, select: (TimePeriod) -> Unit): (() -> Unit)? =
        withCompositionLocals(LocalPeriodState provides periodState, LocalTimeProvider provides clock) {
            backToThisMonth(period, select)
        }

    private companion object {
        val October = TimePeriod(month = Month.fromMonthValue(10), year = 2026)
        val November = TimePeriod(month = Month.fromMonthValue(11), year = 2026)
    }
}
