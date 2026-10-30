package com.ivy.wallet.domain.action.settings

import com.ivy.data.db.dao.read.SettingsDao
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BaseCurrencyAct @Inject constructor(
    private val settingsDao: SettingsDao
) {
    suspend operator fun invoke(): String = withContext(Dispatchers.IO) {
        settingsDao.findFirst().currency
    }
}
