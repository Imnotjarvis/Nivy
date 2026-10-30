package com.ivy.wallet.migrations.impl

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.WorkManager
import com.ivy.data.datastore.dataStore
import com.ivy.wallet.migrations.Migration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DisableGitHubAutoBackupMigration @Inject constructor(
    @ApplicationContext
    private val context: Context,
) : Migration {
    override val key: String
        get() = "disable_github_auto_backups"

    override suspend fun migrate() {
        // Cancel the GitHub worker
        WorkManager.getInstance(context).cancelUniqueWork("GITHUB_AUTO_BACKUP_WORK")

        context.dataStore.edit {
            it.remove(stringPreferencesKey("github_backup_pat"))
            it.remove(stringPreferencesKey("github_backup_repo"))
            it.remove(stringPreferencesKey("github_backup_owner"))
            it.remove(longPreferencesKey("github_backup_last_backup_time_epoch_sec"))
        }
    }
}
