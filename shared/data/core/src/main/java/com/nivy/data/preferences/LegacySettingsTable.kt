package com.nivy.data.preferences

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.NivyRoomDatabase
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The database's old `settings` table. The schema no longer has it; its file kept it, for
 * [LegacySettingsMigration] to copy its values into DataStore.
 */
class LegacySettingsTable @Inject constructor(
    private val db: NivyRoomDatabase,
    private val dispatchers: DispatchersProvider,
) {
    /**
     * Drops the table, or does nothing when it's gone. Only for after DataStore's first read,
     * which is when the values are copied.
     */
    suspend fun drop() {
        withContext(dispatchers.io) {
            db.openHelper.writableDatabase.execSQL("DROP TABLE IF EXISTS settings")
        }
    }
}
