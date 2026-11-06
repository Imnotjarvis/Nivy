package com.ivy.importdata.csv.domain

import com.ivy.importdata.csv.CSVRow
import com.ivy.importdata.csv.ColumnMapping
import io.kotest.matchers.shouldBe
import org.junit.Test

class ParseStatusTest {
    private val rows = listOf(
        CSVRow(listOf("Coffee", "3.20")),
        // A blank line in the file: a row shorter than the mapped column.
        CSVRow(listOf("")),
        CSVRow(listOf("Rent", "800")),
    )

    @Test
    fun `a row shorter than the mapped column is skipped, not the whole column`() {
        // when
        val status = rows.parseStatus(amountColumn(index = 1), ::parseAmount)

        // then
        status.success shouldBe true
        status.sampleValues shouldBe listOf("3.2", "800.0")
    }

    @Test
    fun `a column no row has fails`() {
        // when
        val status = rows.parseStatus(amountColumn(index = 2), ::parseAmount)

        // then
        status.success shouldBe false
        status.sampleValues shouldBe emptyList()
    }

    @Test
    fun `a short row's value is empty when imported`() {
        // when
        val value = rows[1].extractValue(amountColumn(index = 1))

        // then
        value shouldBe ""
    }

    private fun amountColumn(index: Int) = ColumnMapping(
        ivyColumn = 0,
        helpInfo = 0,
        name = "Amount",
        index = index,
        metadata = 1,
        required = true,
    )
}
