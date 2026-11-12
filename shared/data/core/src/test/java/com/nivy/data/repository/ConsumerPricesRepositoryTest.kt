package com.nivy.data.repository

import arrow.core.left
import arrow.core.right
import com.nivy.base.time.TimeProvider
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.InMemoryPreferencesDataStore
import com.nivy.data.remote.RemoteConsumerPricesDataSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class ConsumerPricesRepositoryTest {
    private val remote = mockk<RemoteConsumerPricesDataSource> {
        coEvery { fetchNic(any()) } returns "offline".left()
    }
    private val preferences = AppPreferences(InMemoryPreferencesDataStore())

    // Mid-month, two months after the newest built in: the month between is missing.
    private var now: Instant = Newest.plusMonths(2).atDay(12).atTime(9, 0).toInstant(ZoneOffset.UTC)
    private val time = mockk<TimeProvider> {
        every { utcNow() } answers { now }
        every { localDateNow() } answers { now.atOffset(ZoneOffset.UTC).toLocalDate() }
    }
    private val repository = ConsumerPricesRepository(remote, preferences, time)

    @Test
    fun `the prices are the built-in index, with the months downloaded since`() = runTest {
        // given
        preferences.setConsumerPrices(mapOf(Newest to 120.0, Newest.plusMonths(1) to 121.0))

        // when
        val prices = repository.prices()

        // then: a month downloaded again replaces the one built in
        prices.latest shouldBe Newest.plusMonths(1)
        prices.index[Newest] shouldBe 120.0
        prices.index[YearMonth.of(2020, 1)] shouldBe BuiltInNic[YearMonth.of(2020, 1)]
    }

    @Test
    fun `nothing is downloaded while the index reaches the month before the current one`() = runTest {
        // given ISTAT publishes a month's index at its end
        now -= Duration.ofDays(31)

        // then
        repository.syncIfDue() shouldBe false
        coVerify(exactly = 0) { remote.fetchNic(any()) }
    }

    @Test
    fun `a month missing is downloaded, with the newest again, whose index may have been provisional`() = runTest {
        // given
        coEvery { remote.fetchNic(Newest) } returns mapOf(Newest to 120.0, Newest.plusMonths(1) to 121.0).right()

        // then
        repository.syncIfDue() shouldBe true
        val prices = repository.prices()
        prices.latest shouldBe Newest.plusMonths(1)
        prices.index[Newest] shouldBe 120.0
        // The index now reaches last month: no more downloads this month.
        repository.syncIfDue() shouldBe false
        coVerify(exactly = 1) { remote.fetchNic(any()) }
    }

    @Test
    fun `a download that brings nothing new changes nothing`() = runTest {
        // given ISTAT hasn't published last month's index yet
        coEvery { remote.fetchNic(Newest) } returns mapOf(Newest to BuiltInNic.getValue(Newest)).right()

        // then
        repository.syncIfDue() shouldBe false
        repository.prices().latest shouldBe Newest
    }

    @Test
    fun `a failed download is tried again a day later, not before`() = runTest {
        // given
        repository.syncIfDue() shouldBe false

        // when
        now += Duration.ofHours(23)
        repository.syncIfDue()
        now += Duration.ofHours(1)
        repository.syncIfDue()

        // then
        coVerify(exactly = 2) { remote.fetchNic(any()) }
    }

    @Test
    fun `a download is tried when the clock was set back since the last`() = runTest {
        // given
        repository.syncIfDue()

        // when
        now -= Duration.ofHours(1)
        repository.syncIfDue()

        // then
        coVerify(exactly = 2) { remote.fetchNic(any()) }
    }

    private companion object {
        val Newest: YearMonth = BuiltInNic.keys.max()
    }
}
