package com.ivy.wallet.domain.action.global

import arrow.core.Either
import com.ivy.data.preferences.AppPreferences
import com.ivy.wallet.PeriodState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateStartDayOfMonthActTest {

    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private val periodState = mockk<PeriodState>(relaxed = true)
    private val act = UpdateStartDayOfMonthAct(appPreferences, periodState)

    @Test
    fun `a day of the month becomes the months' first day`() = runTest {
        act(31) shouldBe Either.Right(31)

        coVerify { appPreferences.setStartDayOfMonth(31) }
        verify {
            periodState.setStartDayOfMonth(31)
            periodState.initSelected(startDayOfMonth = 31, force = true)
        }
    }

    @Test
    fun `a day outside the month is refused and not saved`() = runTest {
        act(0).shouldBeInstanceOf<Either.Left<String>>()
        act(32).shouldBeInstanceOf<Either.Left<String>>()

        coVerify(exactly = 0) { appPreferences.setStartDayOfMonth(any()) }
        verify(exactly = 0) { periodState.setStartDayOfMonth(any()) }
    }
}
