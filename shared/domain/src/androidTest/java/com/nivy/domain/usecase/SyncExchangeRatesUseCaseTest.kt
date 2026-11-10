package com.nivy.domain.usecase

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.di.KotlinxSerializationModule
import com.nivy.base.time.impl.DeviceTimeProvider
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.remote.RemoteExchangeRatesDataSource
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.data.repository.mapper.ExchangeRateMapper
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * The download, the conversion to the base currency and the saving, against a recorded response
 * served by Ktor's mock engine: the test doesn't depend on the exchange-rate servers being up.
 */
@RunWith(AndroidJUnit4::class)
class SyncExchangeRatesUseCaseTest {
    private lateinit var useCase: SyncExchangeRatesUseCase
    private lateinit var appPreferences: AppPreferences
    private lateinit var db: NivyRoomDatabase

    /** The URLs asked for, in order. */
    private val requests = mutableListOf<String>()

    /** The URLs that answer with an error. */
    private val down = mutableSetOf<String>()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NivyRoomDatabase::class.java).build()
        val json = KotlinxSerializationModule.provideJson()
        val client = HttpClient(
            MockEngine { request ->
                // As written in RemoteExchangeRatesDataSource, should the "@" come encoded.
                val url = request.url.toString().replace("%40", "@")
                requests += url
                if (url in down) {
                    respondError(HttpStatusCode.ServiceUnavailable)
                } else {
                    respond(EurRates, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
        ) {
            install(ContentNegotiation) { json(json) }
        }

        val repository = ExchangeRatesRepository(
            exchangeRatesDao = db.exchangeRatesDao,
            writeExchangeRatesDao = db.writeExchangeRatesDao,
            mapper = ExchangeRateMapper(),
            remoteExchangeRatesDataSource = RemoteExchangeRatesDataSource(ktorClient = { client }),
            dispatchers = TestDispatchersProvider,
        )

        // A store of its own, so the test leaves the app's settings alone.
        appPreferences = AppPreferences(
            PreferenceDataStoreFactory.create {
                File(context.cacheDir, "sync-exchange-rates-test-${UUID.randomUUID()}.preferences_pb")
            }
        )
        useCase = SyncExchangeRatesUseCase(repository, appPreferences, DeviceTimeProvider())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun syncsExchangeRatesInTheBaseCurrency(): Unit = runBlocking {
        // when
        val res = useCase.sync(AssetCode.USD)

        // then: the euro rates, converted to the dollar
        res.shouldBeRight()
        val saved = db.exchangeRatesDao.findAll().first()
        saved.map { it.baseCurrency }.toSet() shouldBe setOf("USD")
        val rates = saved.associate { it.currency to it.rate }
        rates["EUR"] shouldBe 1 / EurToUsd
        rates["GBP"] shouldBe 0.8512 / EurToUsd
        rates["BGN"] shouldBe 1.95583 / EurToUsd
        rates["USD"] shouldBe 1.0
        requests shouldBe listOf(Urls.first())
        // Remembered, so that the next launches within a day don't download them again.
        appPreferences.exchangeRatesSyncedAt("USD").shouldNotBeNull()
    }

    @Test
    fun aServerThatIsDownIsSkippedForTheNext(): Unit = runBlocking {
        // given
        down += Urls.first()

        // when
        val res = useCase.sync(AssetCode.USD)

        // then
        res.shouldBeRight()
        requests shouldBe Urls.take(2)
        db.exchangeRatesDao.findAll().first().associate { it.currency to it.rate }["EUR"] shouldBe 1 / EurToUsd
    }

    @Test
    fun withNoServerUpNothingIsSavedOrRemembered(): Unit = runBlocking {
        // given
        down += Urls

        // when
        val res = useCase.sync(AssetCode.USD)

        // then: tried every server, and the next launch tries again
        res.shouldBeLeft()
        requests shouldBe Urls
        db.exchangeRatesDao.findAll().first().shouldBeEmpty()
        appPreferences.exchangeRatesSyncedAt("USD").shouldBeNull()
    }

    private companion object {
        const val EurToUsd = 1.0842

        /** The servers asked, in order (RemoteExchangeRatesDataSource). */
        val Urls = listOf(
            "https://currency-api.pages.dev/v1/currencies/eur.json",
            "https://currency-api.pages.dev/v1/currencies/eur.min.json",
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/eur.min.json",
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/eur.json",
        )

        /** A response as the servers send it, cut to a few currencies (one of them a crypto token). */
        val EurRates = """
            {
                "date": "2026-11-05",
                "eur": {
                    "1inch": 4.8613,
                    "bgn": 1.95583,
                    "btc": 0.0000115,
                    "eur": 1,
                    "gbp": 0.8512,
                    "usd": $EurToUsd
                }
            }
        """.trimIndent()
    }
}
