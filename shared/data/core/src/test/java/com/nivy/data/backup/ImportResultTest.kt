package com.nivy.data.backup

import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class ImportResultTest {
    @Test
    fun `an import went well when more than half its rows came in`() {
        result(rowsFound = 10, transactionsImported = 6).succeeded shouldBe true
        result(rowsFound = 10, transactionsImported = 10).succeeded shouldBe true
        result(rowsFound = 1, transactionsImported = 1).succeeded shouldBe true
    }

    @Test
    fun `half the rows or fewer isn't a success`() {
        result(rowsFound = 10, transactionsImported = 5).succeeded shouldBe false
        result(rowsFound = 100, transactionsImported = 10).succeeded shouldBe false
    }

    @Test
    fun `nothing imported isn't a success, whatever the file held`() {
        result(rowsFound = 0, transactionsImported = 0).succeeded shouldBe false
        result(rowsFound = 3, transactionsImported = 0).succeeded shouldBe false
    }

    private fun result(rowsFound: Int, transactionsImported: Int) = ImportResult(
        rowsFound = rowsFound,
        transactionsImported = transactionsImported,
        accountsImported = 0,
        categoriesImported = 0,
        failedRows = persistentListOf(),
    )
}
