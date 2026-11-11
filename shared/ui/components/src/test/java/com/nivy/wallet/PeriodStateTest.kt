package com.nivy.wallet

import com.nivy.base.time.TimeProvider
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.LocalDate

class PeriodStateTest {
    private var today = LocalDate.of(2026, 11, 15)
    private val periodState = PeriodState(mockk<TimeProvider> { every { localDateNow() } answers { today } })

    @Test
    fun `this month picked again follows the months as they change`() {
        periodState.select(October)
        periodState.select(TimePeriod(month = Month.fromMonthValue(11), year = 2026))

        today = LocalDate.of(2026, 12, 1)

        periodState.initSelected(startDayOfMonth = 1) shouldBe TimePeriod(month = Month.fromMonthValue(12), year = 2026)
    }

    @Test
    fun `another month picked stays as the months change`() {
        periodState.select(October)

        today = LocalDate.of(2026, 12, 1)

        periodState.initSelected(startDayOfMonth = 1) shouldBe October
    }

    private companion object {
        val October = TimePeriod(month = Month.fromMonthValue(10), year = 2026)
    }
}
