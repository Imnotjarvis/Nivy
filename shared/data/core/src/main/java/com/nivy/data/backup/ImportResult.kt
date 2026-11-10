package com.nivy.data.backup

import kotlinx.collections.immutable.ImmutableList

data class ImportResult(
    val rowsFound: Int,
    val transactionsImported: Int,
    val accountsImported: Int,
    val categoriesImported: Int,
    val failedRows: ImmutableList<CSVRow>,
    /** Why nothing was imported, when the import failed as a whole. */
    val error: String? = null,
) {
    /**
     * Whether the import went well: more than half the rows came in. The result page says so, and
     * onboarding takes the wallet as restored only then.
     */
    val succeeded: Boolean get() = transactionsImported > 0 && transactionsImported * 2 > rowsFound
}
