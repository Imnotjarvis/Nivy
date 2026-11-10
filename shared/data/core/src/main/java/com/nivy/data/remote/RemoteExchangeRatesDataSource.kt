package com.nivy.data.remote

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import com.nivy.data.remote.responses.ExchangeRatesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

/** Today's rates from euros, from the first of the mirrors that answers. */
class RemoteExchangeRatesDataSource @Inject constructor(
    private val ktorClient: dagger.Lazy<HttpClient>,
) {
    private val urls = listOf(
        "https://currency-api.pages.dev/v1/currencies/eur.json",
        "https://currency-api.pages.dev/v1/currencies/eur.min.json",
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/eur.min.json",
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/eur.json",
    )

    suspend fun fetchEurExchangeRates(): Either<String, ExchangeRatesResponse> {
        var latestResult: Either<String, ExchangeRatesResponse> = "Impossible".left()
        for (url in urls) {
            latestResult = fetchEurExchangeRates(url)
            if (latestResult.isRight()) {
                return latestResult
            }
        }
        return latestResult
    }

    private suspend fun fetchEurExchangeRates(
        url: String
    ): Either<String, ExchangeRatesResponse> = catch({
        Either.Right(ktorClient.get().get(url).body<ExchangeRatesResponse>())
    }) { e ->
        Either.Left(e.message ?: "Error fetching exchange rates")
    }
}
