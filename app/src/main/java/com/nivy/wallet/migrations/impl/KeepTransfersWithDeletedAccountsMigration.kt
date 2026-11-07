package com.nivy.wallet.migrations.impl

import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.db.dao.write.WriteTransactionDao
import com.nivy.data.db.keepTransfersOfDeletedAccounts
import com.nivy.wallet.migrations.Migration
import javax.inject.Inject

/**
 * Repairs, once, the transfers that accounts deleted before left behind, pointing to them
 * (keepTransfersOfDeletedAccounts). Importing a backup repairs those it brings back.
 */
class KeepTransfersWithDeletedAccountsMigration @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val writeTransactionDao: WriteTransactionDao,
) : Migration {
    override val key: String
        get() = "keep_transfers_with_deleted_accounts"

    override suspend fun migrate() {
        keepTransfersOfDeletedAccounts(accountDao, transactionDao, writeTransactionDao)
    }
}
