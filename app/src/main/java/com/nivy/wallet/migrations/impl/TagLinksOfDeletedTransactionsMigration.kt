package com.nivy.wallet.migrations.impl

import com.nivy.data.db.dao.write.WriteTagAssociationDao
import com.nivy.wallet.migrations.Migration
import javax.inject.Inject

/**
 * Deletes, once, the tag links that deleted transactions left behind: a deleted transaction now
 * takes its links with it, and importing a backup drops those it brings.
 */
class TagLinksOfDeletedTransactionsMigration @Inject constructor(
    private val writeTagAssociationDao: WriteTagAssociationDao,
) : Migration {
    override val key: String
        get() = "tag_links_of_deleted_transactions"

    override suspend fun migrate() {
        writeTagAssociationDao.deleteLinksOfDeletedTransactions()
    }
}
