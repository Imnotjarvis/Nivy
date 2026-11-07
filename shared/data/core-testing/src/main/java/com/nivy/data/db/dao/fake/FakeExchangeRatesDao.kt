package com.nivy.data.db.dao.fake

import com.nivy.data.db.dao.read.ExchangeRatesDao
import com.nivy.data.db.dao.write.WriteExchangeRatesDao
import com.nivy.data.db.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
class FakeExchangeRatesDao : ExchangeRatesDao, WriteExchangeRatesDao {
    private val items = MutableStateFlow<List<ExchangeRateEntity>>(emptyList())

    override fun findAll(): Flow<List<ExchangeRateEntity>> = items

    override suspend fun findAllManuallyOverridden(): List<ExchangeRateEntity> =
        items.value.filter { it.manualOverride }

    override suspend fun findByBaseCurrencyAndCurrency(
        baseCurrency: String,
        currency: String
    ): ExchangeRateEntity? = items.value.find { it.baseCurrency == baseCurrency && it.currency == currency }

    override suspend fun save(value: ExchangeRateEntity) {
        deleteByBaseCurrencyAndCurrency(value.baseCurrency, value.currency)
        items.value += value
    }

    override suspend fun saveMany(value: List<ExchangeRateEntity>) {
        value.forEach { save(it) }
    }

    override suspend fun deleteByBaseCurrencyAndCurrency(
        baseCurrency: String,
        currency: String
    ) {
        items.value = items.value.filterNot { it.baseCurrency == baseCurrency && it.currency == currency }
    }

    override suspend fun deleteAll() {
        items.value = emptyList()
    }
}
