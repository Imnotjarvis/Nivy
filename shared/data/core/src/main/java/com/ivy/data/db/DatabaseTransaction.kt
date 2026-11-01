package com.ivy.data.db

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Runs a block of writes as one database transaction (SQLite's, not the app's money
 * transactions): either every write in it lands or none does. Nothing in the block may switch
 * to another dispatcher before touching the database; Room binds the transaction to the
 * coroutine it runs in.
 *
 * A class rather than a direct `db.withTransaction` so tests can replace it with one that just
 * runs the block.
 */
open class DatabaseTransaction @Inject constructor(
    private val db: IvyRoomDatabase,
) {
    open suspend fun <R> run(block: suspend () -> R): R = db.withTransaction(block)
}
