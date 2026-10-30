package com.ivy.legacy.domain.action.settings

import com.ivy.data.db.dao.write.WriteSettingsDao
import com.ivy.legacy.datamodel.Settings
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateSettingsAct @Inject constructor(
    private val writeSettingsDao: WriteSettingsDao
) {
    suspend operator fun invoke(settings: Settings): Settings = withContext(Dispatchers.IO) {
        writeSettingsDao.save(settings.toEntity())
        settings
    }
}
