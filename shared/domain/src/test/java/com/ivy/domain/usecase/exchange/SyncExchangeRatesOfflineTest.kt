package com.ivy.domain.usecase.exchange

import arrow.core.left
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.ExchangeRatesRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncExchangeRatesOfflineTest {
    private val repository = mockk<ExchangeRatesRepository>(relaxed = true)
    private val useCase = SyncExchangeRatesUseCase(repository)

    @Test
    fun `sync - network failure leaves stored rates untouched`() = runTest {
        // given
        val offlineError = "Unable to resolve host \"currency-api.pages.dev\""
        coEvery { repository.fetchEurExchangeRates() } returns offlineError.left()

        // when
        val result = useCase.sync(AssetCode.USD)

        // then
        result.shouldBeLeft() shouldBe offlineError
        coVerify(exactly = 0) { repository.saveManyRates(any()) }
        coVerify(exactly = 0) { repository.deleteAll() }
        coVerify(exactly = 0) { repository.findAllManuallyOverridden() }
    }
}
