package com.nivy.data.remote

import arrow.core.Either
import arrow.core.raise.catch
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import java.time.YearMonth
import java.time.format.DateTimeParseException
import javax.inject.Inject

/** Italy's consumer price index by month, NIC, from ISTAT's SDMX service. */
class RemoteConsumerPricesDataSource @Inject constructor(
    private val ktorClient: dagger.Lazy<HttpClient>,
) {
    /** The index of [from] and of the months after it ISTAT has published, base 2025 = 100. */
    suspend fun fetchNic(from: YearMonth): Either<String, Map<YearMonth, Double>> = catch({
        val csv = ktorClient.get().get(NicUrl) {
            parameter("startPeriod", from.toString())
            // Without it, ISTAT answers in SDMX's XML.
            header(HttpHeaders.Accept, SdmxCsv)
            expectSuccess = true
        }.bodyAsText()
        val index = parseNic(csv)
        if (index.isEmpty()) Either.Left("No index in ISTAT's answer") else Either.Right(index)
    }) { e ->
        Either.Left(e.message ?: "Error fetching consumer prices")
    }
}

// The index by month in an SDMX-CSV answer: its TIME_PERIOD and OBS_VALUE columns.
private fun parseNic(csv: String): Map<YearMonth, Double> {
    val rows = csv.lineSequence().filter { it.isNotBlank() }.map { it.split(',') }.toList()
    // Without either column, none of the rows has a month and an index.
    val header = rows.firstOrNull().orEmpty()
    val period = header.indexOf("TIME_PERIOD")
    val value = header.indexOf("OBS_VALUE")
    return rows.drop(1).mapNotNull { row ->
        val month = row.getOrNull(period)?.let(::yearMonthOrNull)
        val index = row.getOrNull(value)?.toDoubleOrNull()?.takeIf { it > 0 }
        if (month != null && index != null) month to index else null
    }.toMap()
}

private fun yearMonthOrNull(text: String): YearMonth? = try {
    YearMonth.parse(text)
} catch (_: DateTimeParseException) {
    null
}

// The series: monthly, Italy, the index for the whole nation in base 2025 (85), as index numbers
// (4), of all items (00).
private const val NicUrl =
    "https://esploradati.istat.it/SDMXWS/rest/data/IT1,167_745_DF_DCSP_NIC1B2025_1,1.0/M.IT.85.4.00"
private const val SdmxCsv = "application/vnd.sdmx.data+csv;version=1.0.0"
