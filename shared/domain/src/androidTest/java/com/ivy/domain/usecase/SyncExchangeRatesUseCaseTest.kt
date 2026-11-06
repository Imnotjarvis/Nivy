package com.ivy.domain.usecase

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.base.time.impl.DeviceTimeProvider
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.di.KtorClientModule
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.remote.impl.RemoteExchangeRatesDataSourceImpl
import com.ivy.data.repository.ExchangeRatesRepository
import com.ivy.data.repository.mapper.ExchangeRateMapper
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SyncExchangeRatesUseCaseTest {
    private lateinit var useCase: SyncExchangeRatesUseCase
    private lateinit var repository: ExchangeRatesRepository
    private lateinit var appPreferences: AppPreferences
    private lateinit var db: IvyRoomDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, IvyRoomDatabase::class.java).build()

        repository = ExchangeRatesRepository(
            exchangeRatesDao = db.exchangeRatesDao,
            writeExchangeRatesDao = db.writeExchangeRatesDao,
            mapper = ExchangeRateMapper(),
            remoteExchangeRatesDataSource = RemoteExchangeRatesDataSourceImpl(
                ktorClient = {
                    KtorClientModule.provideKtorClient(KotlinxSerializationModule.provideJson())
                },
            ),
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
    fun syncsExchangeRates(): Unit = runBlocking {
        // given
        val exchangeRatesDao = db.exchangeRatesDao

        // when
        val res = useCase.sync(AssetCode.USD)

        // then
        res.shouldBeRight()
        val savedRates = exchangeRatesDao.findAll().first()
        savedRates.shouldNotBeEmpty()
        println("Saved ${savedRates.size} exchange rates")
        val eurRate = savedRates.firstOrNull { it.currency == "EUR" }?.rate
        eurRate.shouldNotBeNull() shouldBeGreaterThan 0.0
        // Remembered, so that the next launches within a day don't download them again.
        appPreferences.exchangeRatesSyncedAt("USD").shouldNotBeNull()
    }
}
