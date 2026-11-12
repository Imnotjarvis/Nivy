package com.nivy.data.remote

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.YearMonth

/** Against answers as ISTAT's service gives them, served by Ktor's mock engine. */
class RemoteConsumerPricesDataSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var answer: MockRequestHandleScope.() -> HttpResponseData = {
        respond(IstatAnswer, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/vnd.sdmx.data+csv"))
    }

    // As the app's client, which asks for JSON too.
    private val client = HttpClient(
        MockEngine { request ->
            requests += request
            answer()
        }
    ) {
        install(ContentNegotiation) { json() }
    }
    private val source = RemoteConsumerPricesDataSource { client }

    @Test
    fun `the index is asked for from a month on, as CSV, and read by month`() = runTest {
        // when
        val index = source.fetchNic(YearMonth.of(2026, 8))

        // then: October's too, though ISTAT hasn't confirmed it yet
        index.shouldBeRight() shouldBe mapOf(
            YearMonth.of(2026, 8) to 103.9,
            YearMonth.of(2026, 9) to 104.6,
            YearMonth.of(2026, 10) to 105.5,
        )
        val request = requests.single()
        request.url.toString() shouldBe
            "https://esploradati.istat.it/SDMXWS/rest/data/IT1,167_745_DF_DCSP_NIC1B2025_1,1.0/M.IT.85.4.00" +
            "?startPeriod=2026-08"
        // First: ISTAT answers with the first type asked for that it has.
        request.headers.getAll(HttpHeaders.Accept)?.first() shouldBe "application/vnd.sdmx.data+csv;version=1.0.0"
    }

    @Test
    fun `an error is a failure`() = runTest {
        // given ISTAT's answer to a month it has nothing from
        answer = { respondError(HttpStatusCode.NotFound, "NoRecordsFound") }

        // then
        source.fetchNic(YearMonth.of(2026, 12)).shouldBeLeft()
    }

    @Test
    fun `an answer without the index is a failure`() = runTest {
        // given SDMX-ML, as ISTAT answers when CSV isn't asked for
        answer = {
            respond(
                "<message:GenericData><message:DataSet/></message:GenericData>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/xml"),
            )
        }

        // then
        source.fetchNic(YearMonth.of(2026, 8)).shouldBeLeft()
    }

    private companion object {
        /** ISTAT's answer from August 2026, on November 12, 2026: October's is provisional (p). */
        val IstatAnswer = """
            DATAFLOW,FREQ,REF_AREA,DATA_TYPE,MEASURE,ECOICOP_2,TIME_PERIOD,OBS_VALUE,OBS_STATUS,NOTE_DS,NOTE_REF_AREA,NOTE_DATA_TYPE,NOTE_MEASURE,NOTE_ECOICOP_2,NOTE_TIME_PERIOD,BASE_PER,UNIT_MEAS,UNIT_MULT
            IT1:167_745_DF_DCSP_NIC1B2025_1(1.0),M,IT,85,4,00,2026-08,103.9,,,,,,,,,,
            IT1:167_745_DF_DCSP_NIC1B2025_1(1.0),M,IT,85,4,00,2026-09,104.6,,,,,,,,,,
            IT1:167_745_DF_DCSP_NIC1B2025_1(1.0),M,IT,85,4,00,2026-10,105.5,p,,,,,,,,,
        """.trimIndent().replace("\n", "\r\n")
    }
}
