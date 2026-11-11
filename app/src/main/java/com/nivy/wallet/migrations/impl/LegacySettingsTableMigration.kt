package com.nivy.wallet.migrations.impl

import com.nivy.data.preferences.LegacySettingsTable
import com.nivy.wallet.migrations.Migration
import javax.inject.Inject

/**
 * Drops, once, the database's old settings table. Its values are in DataStore by now: they're
 * copied before DataStore's first read, and these migrations start by reading it.
 */
class LegacySettingsTableMigration @Inject constructor(
    private val legacySettingsTable: LegacySettingsTable,
) : Migration {
    override val key: String
        get() = "drop_legacy_settings_table"

    override suspend fun migrate() {
        legacySettingsTable.drop()
    }
}
