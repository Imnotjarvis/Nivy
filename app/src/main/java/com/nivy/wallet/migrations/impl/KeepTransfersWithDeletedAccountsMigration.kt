package com.nivy.wallet.migrations.impl

import com.nivy.base.model.TransactionType
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.db.dao.write.WriteTransactionDao
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.wallet.migrations.Migration
import java.util.UUID
import javax.inject.Inject

/**
 * Deleting an account used to leave behind the transfers other accounts had made to it, still
 * pointing to it. A transfer counts only between two accounts that exist, so they counted for no
 * account: the paying account's balance was as if they were never paid. Each becomes the paying
 * account's expense, noting where it went. A transfer from an account that's gone into one still
 * there becomes the receiving account's income. Deleting an account now keeps its transfers itself
 * (AccountCategoryDeletion); this repairs the ones left by accounts deleted before.
 */
class KeepTransfersWithDeletedAccountsMigration @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val writeTransactionDao: WriteTransactionDao,
) : Migration {
    override val key: String
        get() = "keep_transfers_with_deleted_accounts"

    override suspend fun migrate() {
        val accounts = accountDao.findAll().map { it.id }.toSet()
        val kept = transactionDao.findAll().mapNotNull { it.keptWith(accounts) }
        if (kept.isNotEmpty()) writeTransactionDao.saveMany(kept)
    }
}

/** This transfer as a plain transaction of its account still in [accounts], or null to leave it. */
private fun TransactionEntity.keptWith(accounts: Set<UUID>): TransactionEntity? {
    val to = toAccountId
    if (type != TransactionType.TRANSFER || to == null) return null
    val fromThere = accountId in accounts
    val toThere = to in accounts
    return when {
        fromThere && !toThere -> copy(
            type = TransactionType.EXPENSE,
            toAccountId = null,
            toAmount = null,
            description = description.withNote("Transfer to a deleted account"),
        )

        toThere && !fromThere -> copy(
            type = TransactionType.INCOME,
            accountId = to,
            // What the receiving account got, as its balance counted it.
            amount = toAmount?.takeIf { it > 0 && it.isFinite() } ?: amount,
            toAccountId = null,
            toAmount = null,
            description = description.withNote("Transfer from a deleted account"),
        )

        // Both accounts there is a transfer as it should be; both gone, it belongs to no account.
        else -> null
    }
}

/** The description, with [note] on a line of its own after it. */
private fun String?.withNote(note: String): String =
    listOfNotNull(this?.takeIf { it.isNotBlank() }, note).joinToString("\n")
