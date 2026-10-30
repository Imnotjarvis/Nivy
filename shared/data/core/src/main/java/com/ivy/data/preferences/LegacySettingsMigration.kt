package com.ivy.data.preferences

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.ivy.data.db.IvyRoomDatabase
import timber.log.Timber

/**
 * Copies the settings from the database's old `settings` table into DataStore, once.
 *
 * It reads the database file directly because DataStore runs it before its first read, which
 * can come before Room opens the database. The table itself stays in the file: the database
 * schema no longer uses it, and DeleteAllDataUseCase drops it.
 */
class LegacySettingsMigration(private val context: Context) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[Migrated] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        readLegacySettings()?.let { legacy ->
            prefs[AppPreferences.ThemeName] = legacy.theme
            prefs[AppPreferences.BaseCurrency] = legacy.currency
            prefs[AppPreferences.BufferAmount] = legacy.bufferAmount
            prefs[AppPreferences.UserName] = legacy.name
        }
        prefs[Migrated] = true
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() = Unit

    private fun readLegacySettings(): LegacySettings? {
        val dbFile = context.getDatabasePath(IvyRoomDatabase.DB_NAME)
        if (!dbFile.exists()) return null
        return try {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                .use(::readFirstSettingsRow)
        } catch (e: SQLiteException) {
            // no settings table: a fresh install, or the user deleted all data
            Timber.i(e, "No legacy settings to migrate")
            null
        }
    }

    private fun readFirstSettingsRow(db: SQLiteDatabase): LegacySettings? =
        db.rawQuery("SELECT theme, currency, bufferAmount, name FROM settings LIMIT 1", null).use { cursor ->
            cursor.takeIf { it.moveToFirst() }?.let {
                LegacySettings(
                    theme = it.getString(it.getColumnIndexOrThrow("theme")),
                    currency = it.getString(it.getColumnIndexOrThrow("currency")),
                    bufferAmount = it.getDouble(it.getColumnIndexOrThrow("bufferAmount")),
                    name = it.getString(it.getColumnIndexOrThrow("name")).orEmpty(),
                )
            }
        }

    private class LegacySettings(
        val theme: String,
        val currency: String,
        val bufferAmount: Double,
        val name: String,
    )

    companion object {
        /** Kept by DeleteAllDataUseCase, so the copy doesn't run again. */
        val Migrated = booleanPreferencesKey("settings_migrated_from_database")
    }
}
