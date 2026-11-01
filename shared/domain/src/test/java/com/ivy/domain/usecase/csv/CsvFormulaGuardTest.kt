package com.ivy.domain.usecase.csv

import io.kotest.matchers.shouldBe
import org.junit.Test

class CsvFormulaGuardTest {

    @Test
    fun `cells that spreadsheets would run as formulas get an apostrophe`() {
        listOf("=SUM(A1:A9)", "+1", "-5 bucks", "@cmd", "\tx", "\rx").forEach { cell ->
            CsvFormulaGuard.escape(cell) shouldBe "'$cell"
        }
    }

    @Test
    fun `other cells are left alone`() {
        listOf("", "Groceries", "5", " -5", "'quoted'", "Café").forEach { cell ->
            CsvFormulaGuard.escape(cell) shouldBe cell
        }
    }

    @Test
    fun `a cell that already looks escaped gets a second apostrophe`() {
        CsvFormulaGuard.escape("'=1") shouldBe "''=1"
        CsvFormulaGuard.escape("''-5") shouldBe "'" + "''-5"
    }

    @Test
    fun `unescape only removes the apostrophe the guard added`() {
        CsvFormulaGuard.unescape("'=SUM(A1)") shouldBe "=SUM(A1)"
        CsvFormulaGuard.unescape("''=1") shouldBe "'=1"
        CsvFormulaGuard.unescape("'-5") shouldBe "-5"
        CsvFormulaGuard.unescape("'quoted'") shouldBe "'quoted'"
        CsvFormulaGuard.unescape("'") shouldBe "'"
        CsvFormulaGuard.unescape("-5") shouldBe "-5"
    }

    @Test
    fun `escape then unescape is the identity`() {
        listOf("=1+1", "-5", "'=1", "plain", "", "@x", "a\\b").forEach { cell ->
            CsvFormulaGuard.unescape(CsvFormulaGuard.escape(cell)) shouldBe cell
        }
    }
}
