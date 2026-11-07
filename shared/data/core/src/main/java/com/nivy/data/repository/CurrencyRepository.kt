package com.nivy.data.repository

import android.icu.util.Currency
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.preferences.AppPreferences
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepository @Inject constructor(
    private val appPreferences: AppPreferences,
    private val dispatchersProvider: DispatchersProvider,
) {
    companion object {
        const val FALLBACK_DEFAULT_CURRENCY = "USD"
    }

    suspend fun getBaseCurrency(): AssetCode = withContext(dispatchersProvider.io) {
        val currencyCode = appPreferences.baseCurrency()?.takeIf { it.isNotBlank() }
            ?: getDefaultFIATCurrency()?.currencyCode
        currencyCode?.let(AssetCode::from)?.getOrNull()
            ?: AssetCode.unsafe(FALLBACK_DEFAULT_CURRENCY)
    }

    private fun getDefaultFIATCurrency(): Currency? {
        return Currency.getInstance(Locale.getDefault())
    }
}
