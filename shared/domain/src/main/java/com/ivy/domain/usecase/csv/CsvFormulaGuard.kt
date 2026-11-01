package com.ivy.domain.usecase.csv

/**
 * Keeps an exported cell from running as a formula in the spreadsheet that opens the file.
 *
 * A cell that starts with `=`, `+`, `-`, `@`, a tab or a carriage return is prefixed with an
 * apostrophe, which spreadsheets show as text. A cell that already starts with an apostrophe
 * followed by one of those gets another, so that [unescape] is the exact inverse of [escape] and
 * [ReadCsvUseCase] gives back the values the app exported.
 */
object CsvFormulaGuard {
    private const val MARK = '\''
    private val triggers = setOf('=', '+', '-', '@', '\t', '\r')

    fun escape(cell: String): String = if (needsMark(cell)) MARK + cell else cell

    fun unescape(cell: String): String =
        if (cell.startsWith(MARK) && needsMark(cell.substring(1))) cell.substring(1) else cell

    /** True when the cell, with any leading marks taken off, would start a formula. */
    private fun needsMark(cell: String): Boolean =
        cell.trimStart(MARK).firstOrNull()?.let { it in triggers } == true
}
