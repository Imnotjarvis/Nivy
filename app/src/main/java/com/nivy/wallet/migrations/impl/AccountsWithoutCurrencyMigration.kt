package com.nivy.wallet.migrations.impl

import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.db.dao.write.WriteAccountDao
import com.nivy.data.db.giveAccountsWithoutCurrency
import com.nivy.data.repository.CurrencyRepository
import com.nivy.wallet.migrations.Migration
import javax.inject.Inject

/**
 * Gives the accounts stored without a currency the base currency, once (giveAccountsWithoutCurrency).
 * Importing a backup gives those it brings the backup's.
 */
class AccountsWithoutCurrencyMigration @Inject constructor(
    private val accountDao: AccountDao,
    private val writeAccountDao: WriteAccountDao,
    private val currencyRepository: CurrencyRepository,
    private val dataObserver: DataObserver,
) : Migration {
    override val key: String
        get() = "accounts_without_currency"

    override suspend fun migrate() {
        val given = giveAccountsWithoutCurrency(currencyRepository.getBaseCurrency().code, accountDao, writeAccountDao)
        // The accounts were read with the base currency, as they now are stored; read them again.
        if (given > 0) dataObserver.post(DataWriteEvent.AccountsChanged)
    }
}
