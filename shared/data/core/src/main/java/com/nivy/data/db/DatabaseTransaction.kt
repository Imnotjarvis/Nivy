package com.nivy.data.db

import androidx.room.withTransaction
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
    private val dataObserver: DataObserver,
) {
    /**
     * Runs [block] in one transaction. Cut short, by a failure or a cancellation, none of its
     * writes stay, and the repositories' memos, which they updated, read the database again.
     */
    @Suppress("TooGenericExceptionCaught")
    open suspend fun <R> run(block: suspend () -> R): R = try {
        db.withTransaction(block)
    } catch (e: Throwable) {
        withContext(NonCancellable) { dataObserver.post(DataWriteEvent.AllDataChange) }
        throw e
    }
}
