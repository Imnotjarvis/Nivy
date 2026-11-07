package com.nivy.wallet.domain.data

import android.icu.util.Currency
import androidx.compose.runtime.Immutable
import com.nivy.wallet.utils.getDefaultFIATCurrency
import timber.log.Timber

@Immutable
data class NivyCurrency(
    val code: String,
    val name: String,
    val isCrypto: Boolean
) {
    companion object {
        private const val CRYPTO_DECIMAL = 18
        private const val FIAT_DECIMAL = 2

        private val CRYPTO = setOf(
            NivyCurrency(
                code = "BTC",
                name = "Bitcoin",
                isCrypto = true
            ),
            NivyCurrency(
                code = "ETH",
                name = "Ethereum",
                isCrypto = true
            ),
            NivyCurrency(
                code = "USDT",
                name = "Tether USD",
                isCrypto = true
            ),
            NivyCurrency(
                code = "BNB",
                name = "Binance Coin",
                isCrypto = true
            ),
            NivyCurrency(
                code = "ADA",
                name = "Cardano",
                isCrypto = true
            ),
            NivyCurrency(
                code = "XRP",
                name = "Ripple",
                isCrypto = true
            ),
            NivyCurrency(
                code = "DOGE",
                name = "Dogecoin",
                isCrypto = true
            ),
            NivyCurrency(
                code = "USDC",
                name = "USD Coin",
                isCrypto = true
            ),
            NivyCurrency(
                code = "DOT",
                name = "Polkadot",
                isCrypto = true
            ),
            NivyCurrency(
                code = "UNI",
                name = "Uniswap",
                isCrypto = true
            ),
            NivyCurrency(
                code = "BUSD",
                name = "Binance USD",
                isCrypto = true
            ),
            NivyCurrency(
                code = "BCH",
                name = "Bitcoin Cash",
                isCrypto = true
            ),
            NivyCurrency(
                code = "SOL",
                name = "Solana",
                isCrypto = true
            ),
            NivyCurrency(
                code = "LTC",
                name = "Litecoin",
                isCrypto = true
            ),
            NivyCurrency(
                code = "LINK",
                name = "ChainLink Token",
                isCrypto = true
            ),
            NivyCurrency(
                code = "SHIB",
                name = "Shiba Inu coin",
                isCrypto = true
            ),
            NivyCurrency(
                code = "LUNA",
                name = "Terra",
                isCrypto = true
            ),
            NivyCurrency(
                code = "AVAX",
                name = "Avalanche",
                isCrypto = true
            ),
            NivyCurrency(
                code = "MATIC",
                name = "Polygon",
                isCrypto = true
            ),
            NivyCurrency(
                code = "CRO",
                name = "Cronos",
                isCrypto = true
            ),
            NivyCurrency(
                code = "WBTC",
                name = "Wrapped Bitcoin",
                isCrypto = true
            ),
            NivyCurrency(
                code = "ALGO",
                name = "Algorand",
                isCrypto = true
            ),
            NivyCurrency(
                code = "XLM",
                name = "Stellar",
                isCrypto = true
            ),
            NivyCurrency(
                code = "MANA",
                name = "Decentraland",
                isCrypto = true
            ),
            NivyCurrency(
                code = "AXS",
                name = "Axie Infinity",
                isCrypto = true
            ),
            NivyCurrency(
                code = "DAI",
                name = "Dai",
                isCrypto = true
            ),
            NivyCurrency(
                code = "ICP",
                name = "Internet Computer",
                isCrypto = true
            ),
            NivyCurrency(
                code = "ATOM",
                name = "Cosmos",
                isCrypto = true
            ),
            NivyCurrency(
                code = "FIL",
                name = "Filecoin",
                isCrypto = true
            ),
            NivyCurrency(
                code = "ETC",
                name = "Ethereum Classic",
                isCrypto = true
            ),
            NivyCurrency(
                code = "DASH",
                name = "Dash",
                isCrypto = true
            ),
            NivyCurrency(
                code = "TRX",
                name = "Tron",
                isCrypto = true
            ),
            NivyCurrency(
                code = "TON",
                name = "Tonchain",
                isCrypto = true
            ),
        )

        fun getAvailable(): List<NivyCurrency> {
            return Currency.getAvailableCurrencies()
                .map {
                    NivyCurrency(
                        code = it.currencyCode,
                        name = it.displayName,
                        isCrypto = false
                    )
                }
                .plus(CRYPTO)
        }

        fun fromCode(code: String): NivyCurrency? {
            if (code.isBlank()) return null

            val crypto = CRYPTO.find { it.code == code }
            if (crypto != null) {
                return crypto
            }

            return try {
                val fiat = Currency.getInstance(code)
                NivyCurrency(
                    fiatCurrency = fiat
                )
            } catch (e: IllegalArgumentException) {
                // Not an ISO 4217 code the device knows.
                Timber.e(e)
                null
            }
        }

        fun getDefault(): NivyCurrency = NivyCurrency(
            fiatCurrency = getDefaultFIATCurrency()
        )

        fun getDecimalPlaces(assetCode: String): Int =
            if (fromCode(assetCode) in CRYPTO) CRYPTO_DECIMAL else FIAT_DECIMAL
    }

    constructor(fiatCurrency: Currency) : this(
        code = fiatCurrency.currencyCode,
        name = fiatCurrency.displayName,
        isCrypto = false
    )
}
