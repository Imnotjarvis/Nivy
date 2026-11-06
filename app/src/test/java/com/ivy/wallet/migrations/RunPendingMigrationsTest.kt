package com.ivy.wallet.migrations

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RunPendingMigrationsTest {
    private val dataStore = InMemoryDataStore()

    @Test
    fun `a migration runs once`() = runTest {
        val migration = CountingMigration("a")

        runPendingMigrations(listOf(migration), dataStore)
        runPendingMigrations(listOf(migration), dataStore)

        migration.runs shouldBe 1
    }

    @Test
    fun `a migration that failed runs again on the next launch`() = runTest {
        val failsOnce = CountingMigration("fails-once", failures = 1)
        val succeeds = CountingMigration("succeeds")

        runPendingMigrations(listOf(failsOnce, succeeds), dataStore)
        runPendingMigrations(listOf(failsOnce, succeeds), dataStore)
        runPendingMigrations(listOf(failsOnce, succeeds), dataStore)

        // Failed, then succeeded the next time, and done since.
        failsOnce.runs shouldBe 2
        // A failure before it doesn't stop it.
        succeeds.runs shouldBe 1
    }

    @Test
    fun `a cancelled migration stops the run and isn't marked done`() = runTest {
        val cancelled = CountingMigration("cancelled", cancellations = 1)

        shouldThrow<CancellationException> { runPendingMigrations(listOf(cancelled), dataStore) }
        runPendingMigrations(listOf(cancelled), dataStore)

        cancelled.runs shouldBe 2
    }

    private class CountingMigration(
        override val key: String,
        private var failures: Int = 0,
        private var cancellations: Int = 0,
    ) : Migration {
        var runs = 0

        override suspend fun migrate() {
            runs++
            if (cancellations > 0) {
                cancellations--
                throw CancellationException("cancelled")
            }
            if (failures > 0) {
                failures--
                error("failed")
            }
        }
    }

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
