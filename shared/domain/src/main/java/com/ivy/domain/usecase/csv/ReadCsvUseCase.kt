package com.ivy.domain.usecase.csv

import com.opencsv.CSVReaderBuilder
import com.opencsv.RFC4180ParserBuilder
import com.opencsv.validators.LineValidator
import com.opencsv.validators.RowValidator
import java.io.StringReader
import javax.inject.Inject

/**
 * The one CSV reader of the app. Parses per RFC 4180 (quotes are the only escaping, so
 * backslashes in the data survive), accepts every line and row, and undoes the formula guard
 * [ExportCsvUseCase] adds, so an export of the app re-imports unchanged.
 */
class ReadCsvUseCase @Inject constructor() {

    fun readCsv(csv: String, skipLines: Int = 0): List<List<String>> {
        val csvReader = CSVReaderBuilder(StringReader(csv))
            .withCSVParser(RFC4180ParserBuilder().build())
            .withSkipLines(skipLines)
            .withLineValidator(object : LineValidator {
                override fun isValid(line: String?): Boolean {
                    return true
                }

                override fun validate(line: String?) {
                    // do nothing
                }
            })
            .withRowValidator(object : RowValidator {
                override fun isValid(row: Array<out String>?): Boolean {
                    return true
                }

                override fun validate(row: Array<out String>?) {
                    // do nothing
                }
            })
            .build()

        return csvReader.readAll()
            .map { row -> row.map(CsvFormulaGuard::unescape) }
    }
}
