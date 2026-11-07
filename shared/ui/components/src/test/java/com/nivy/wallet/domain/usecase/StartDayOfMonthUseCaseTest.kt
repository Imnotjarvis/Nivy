package com.nivy.wallet.domain.usecase

import arrow.core.Either
import com.nivy.data.preferences.AppPreferences
import com.nivy.wallet.PeriodState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StartDayOfMonthUseCaseTest {

    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private val periodState = mockk<PeriodState>(relaxed = true)
    private val useCase = StartDayOfMonthUseCase(appPreferences, periodState)

    @Test
    fun `a day of the month becomes the months' first day`() = runTest {
        useCase.setStartDay(31) shouldBe Either.Right(31)

        coVerify { appPreferences.setStartDayOfMonth(31) }
        verify {
            periodState.setStartDayOfMonth(31)
            periodState.initSelected(startDayOfMonth = 31, force = true)
        }
    }

    @Test
    fun `a day outside the month is refused and not saved`() = runTest {
        useCase.setStartDay(0).shouldBeInstanceOf<Either.Left<String>>()
        useCase.setStartDay(32).shouldBeInstanceOf<Either.Left<String>>()

        coVerify(exactly = 0) { appPreferences.setStartDayOfMonth(any()) }
        verify(exactly = 0) { periodState.setStartDayOfMonth(any()) }
    }
}
