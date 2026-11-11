package com.nivy.importdata.csv.domain

import com.nivy.importdata.csv.DateMetadata
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDateTime

class ParseFieldsTest {
    private fun amount(value: String) = parseAmount(value, metadata = 1)

    // From /10,000 to *10,000, through 1 for an amount left as it is.
    private val multipliers = listOf(-10_000, -1_000, -100, -10, 1, 10, 100, 1_000, 10_000)

    @Test
    fun `the amount's multiplier steps by tens, through none`() {
        multipliers.zipWithNext { lower, higher ->
            multipliedByTen(lower) shouldBe higher
            dividedByTen(higher) shouldBe lower
        }
    }

    @Test
    fun `the multiplier stops where amounts stop being scaled`() {
        multipliedByTen(10_000) shouldBe 10_000
        dividedByTen(-10_000) shouldBe -10_000
    }

    @Test
    fun `every multiplier the steps reach scales the amount`() {
        multipliers.map { parseAmount("2", metadata = it) } shouldBe
            listOf(0.0002, 0.002, 0.02, 0.2, 2.0, 20.0, 200.0, 2_000.0, 20_000.0)
    }

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
    fun `a column's amounts that can only be decimal tell how its others read`() {
        decimalSeparatorOf(listOf("-12.0", "-4.125")) shouldBe '.'
        decimalSeparatorOf(listOf("1.234", "12,50")) shouldBe ','
        decimalSeparatorOf(listOf("1,234.56", "0,125")) shouldBe null // they disagree
        decimalSeparatorOf(listOf("1,234", "5", "")) shouldBe null // none tells

        parseAmount("-4.125", metadata = 1, decimalSeparator = '.') shouldBe 4.125
        parseAmount("4,125", metadata = 1, decimalSeparator = '.') shouldBe 4125.0
        parseAmount("1.234.567", metadata = 1, decimalSeparator = '.') shouldBe 1234567.0
        parseAmount("12,5", metadata = 1, decimalSeparator = '.') shouldBe 12.5
    }

    @Test
    fun `a time with fractions of a second is read to the second`() {
        // As Cashew writes every date
        parseDate("2026-10-05 13:00:00.000", DateMetadata.DateFirst) shouldBe LocalDateTime.of(2026, 10, 5, 13, 0)
        parseDate("2026-10-05 13:07:09.123456", DateMetadata.MonthFirst) shouldBe
            LocalDateTime.of(2026, 10, 5, 13, 7, 9)
        parseDate("05/10/2026 13:07:09,5", DateMetadata.DateFirst) shouldBe LocalDateTime.of(2026, 10, 5, 13, 7, 9)
    }

    @Test
    fun `day-first files still read year-first dates as ISO`() {
        parseDate("2024-03-05", DateMetadata.DateFirst) shouldBe LocalDateTime.of(2024, 3, 5, 0, 0)
        parseDate("2024-03-25 10:30", DateMetadata.DateFirst) shouldBe LocalDateTime.of(2024, 3, 25, 10, 30)
        parseDate("05/03/2024", DateMetadata.DateFirst) shouldBe LocalDateTime.of(2024, 3, 5, 0, 0)
    }
}
