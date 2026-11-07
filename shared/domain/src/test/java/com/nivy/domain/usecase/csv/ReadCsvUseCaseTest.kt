package com.nivy.domain.usecase.csv

import io.kotest.matchers.shouldBe
import org.junit.Test

class ReadCsvUseCaseTest {

    private val readCsv = ReadCsvUseCase()

    @Test
    fun `commas separate values, and quotes keep a comma inside one`() {
        // when
        val rows = readCsv.readCsv("Amount,Title\n12.50,\"Coffee, croissant\"\n")

        // then
        rows shouldBe listOf(listOf("Amount", "Title"), listOf("12.50", "Coffee, croissant"))
    }

    @Test
    fun `with semicolons, a decimal comma stays in its value`() {
        // when
        val rows = readCsv.readCsv("Amount;Title\n12,50;\"Coffee; croissant\"\n", separator = ';')

        // then
        rows shouldBe listOf(listOf("Amount", "Title"), listOf("12,50", "Coffee; croissant"))
    }
}
