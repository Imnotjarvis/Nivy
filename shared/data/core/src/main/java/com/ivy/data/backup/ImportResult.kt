package com.ivy.data.backup

import kotlinx.collections.immutable.ImmutableList

data class ImportResult(
    val rowsFound: Int,
    val transactionsImported: Int,
    val accountsImported: Int,
    val categoriesImported: Int,
    val failedRows: ImmutableList<CSVRow>,
    /** Why nothing was imported, when the import failed as a whole. */
    val error: String? = null,
)
