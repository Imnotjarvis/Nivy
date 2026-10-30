package com.ivy.wallet.domain.action.settings

import com.ivy.base.model.Theme
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.legacy.datamodel.Settings
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsAct @Inject constructor(
    private val settingsDao: SettingsDao
) {
    suspend operator fun invoke(): Settings = withContext(Dispatchers.IO) {
        settingsDao.findFirst().toLegacyDomain()
    }

    suspend fun getSettingsWithNextTheme(): Settings {
        val currentSettings = this()
        val newTheme = when (currentSettings.theme) {
            Theme.LIGHT -> Theme.DARK
            Theme.DARK -> Theme.AMOLED_DARK
            Theme.AMOLED_DARK -> Theme.AUTO
            Theme.AUTO -> Theme.LIGHT
        }
        return currentSettings.copy(theme = newTheme)
    }

    suspend fun getSettings(): Settings = this()
}