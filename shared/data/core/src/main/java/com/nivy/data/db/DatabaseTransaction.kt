package com.nivy.data.db

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Runs a block of writes as one database transaction (SQLite's, not the app's money
 * transactions): either every write in it lands or none does. Room carries the transaction in
 * the coroutine's context, so the block's writes are in it whichever dispatcher they run on, as
 * repositories switch to the IO one (DatabaseTransactionTest).
 *
 * A class rather than a direct `db.withTransaction` so tests can replace it with one that just
 * runs the block.
 */
open class DatabaseTransaction @Inject constructor(
    private val db: NivyRoomDatabase,
) {
    open suspend fun <R> run(block: suspend () -> R): R = db.withTransaction(block)
}
