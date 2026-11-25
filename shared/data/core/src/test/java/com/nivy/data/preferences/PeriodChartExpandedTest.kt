package com.nivy.data.preferences

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PeriodChartExpandedTest {
    private val preferences = AppPreferences(InMemoryPreferencesDataStore())

    @Test
    fun `a page's chart of the period is open until it's folded, then stays folded`() = runTest {
        preferences.periodChartExpanded() shouldBe true

        preferences.setPeriodChartExpanded(false)

        preferences.periodChartExpanded() shouldBe false
    }
}
