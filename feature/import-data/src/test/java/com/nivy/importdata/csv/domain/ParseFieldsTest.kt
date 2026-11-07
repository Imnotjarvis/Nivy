package com.nivy.importdata.csv.domain

import com.nivy.importdata.csv.DateMetadata
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDateTime

class ParseFieldsTest {
    private fun amount(value: String) = parseAmount(value, metadata = 1)

    @Test
    fun `decimal commas and the usual grouping separators`() {
        amount("12,50") shouldBe 12.5
        amount("1 234,56") shouldBe 1234.56
        amount("1.234,56") shouldBe 1234.56
        amount("1,234.56") shouldBe 1234.56
        amount("1'234.56") shouldBe 1234.56
        amount("-45.10") shouldBe 45.1
        amount("1234") shouldBe 1234.0
    }

    @Test
    fun `a lone separator before three digits groups thousands, except after a zero`() {
        amount("1,234") shouldBe 1234.0
        amount("1.234.567") shouldBe 1234567.0
        amount("0,125") shouldBe 0.125
    }

    @Test
    fun `day-first files still read year-first dates as ISO`() {
        parseDate("2024-03-05", DateMetadata.DateFirst) shouldBe LocalDateTime.of(2024, 3, 5, 0, 0)
        parseDate("2024-03-25 10:30", DateMetadata.DateFirst) shouldBe LocalDateTime.of(2024, 3, 25, 10, 30)
        parseDate("05/03/2024", DateMetadata.DateFirst) shouldBe LocalDateTime.of(2024, 3, 5, 0, 0)
    }
}
