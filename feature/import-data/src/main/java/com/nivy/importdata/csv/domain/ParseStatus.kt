package com.nivy.importdata.csv.domain

import com.nivy.importdata.csv.CSVRow
import com.nivy.importdata.csv.ColumnMapping
import com.nivy.importdata.csv.MappingStatus

const val SAMPLE_SIZE = 20

fun <T, M> List<CSVRow>.parseStatus(
    mapping: ColumnMapping<M>,
    parse: (String, M) -> T?
): MappingStatus {
    // A row shorter than the mapped column has no value there, as the import reads it.
    val parsed = this.mapNotNull { row ->
        row.values.getOrNull(mapping.index)?.let { parse(it, mapping.metadata) }
    }

    return MappingStatus(
        sampleValues = parsed.map { it.toString() },
        success = parsed.isNotEmpty()
    )
}
