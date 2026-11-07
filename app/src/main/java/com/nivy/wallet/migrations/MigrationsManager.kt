package com.nivy.wallet.migrations

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.nivy.data.datastore.dataStore
import com.nivy.wallet.migrations.impl.DisableGitHubAutoBackupMigration
import com.nivy.wallet.migrations.impl.KeepTransfersWithDeletedAccountsMigration
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class MigrationsManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val disableGitHubAutoBackupMigration: Lazy<DisableGitHubAutoBackupMigration>,
    private val keepTransfersWithDeletedAccountsMigration: Lazy<KeepTransfersWithDeletedAccountsMigration>,
) {
    private val migrations by lazy {
        listOf(
            disableGitHubAutoBackupMigration.get(),
            keepTransfersWithDeletedAccountsMigration.get(),
        )
    }

    @Suppress("MagicNumber")
    suspend fun executeMigrations() {
        delay(2_000L) // to not the make the app start slower
        runPendingMigrations(migrations, context.dataStore)
    }
}

/**
 * Runs each migration not done yet, and marks it done. One that fails isn't marked: it runs
 * again on the next launch (it used to be marked done all the same, and never ran again).
 */
internal suspend fun runPendingMigrations(migrations: List<Migration>, dataStore: DataStore<Preferences>) {
    val data = dataStore.data.firstOrNull()
    for (migration in migrations) {
        val key = booleanPreferencesKey("migration_${migration.key}")
        val isExecuted = data?.get(key) ?: false
        if (!isExecuted && migrate(migration)) {
            dataStore.edit { it[key] = true }
        }
    }
}

/** False when [migration] failed. */
@Suppress("TooGenericExceptionCaught")
private suspend fun migrate(migration: Migration): Boolean {
    Timber.i("[MIGRATION] Executing '${migration.key}' migration...")
    return try {
        withContext(Dispatchers.IO) {
            migration.migrate()
        }
        Timber.i("[MIGRATION] Finished '${migration.key}' migration.")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "[MIGRATION] '${migration.key}' failed: it runs again on the next launch")
        false
    }
}
