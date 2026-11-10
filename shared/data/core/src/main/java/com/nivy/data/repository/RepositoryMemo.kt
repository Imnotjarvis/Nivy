package com.nivy.data.repository

import com.nivy.base.di.AppCoroutineScope
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.model.sync.Identifiable
import com.nivy.data.model.sync.UniqueId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class RepositoryMemoFactory @Inject constructor(
    private val dataObserver: DataObserver,
    private val dispatchers: DispatchersProvider,
    @AppCoroutineScope private val appScope: CoroutineScope,
) {
    /** A memo whose writes post [writeEvent], when something reacts to them. */
    fun <T : Identifiable<TID>, TID : UniqueId> createMemo(
        writeEvent: DataWriteEvent? = null,
    ): RepositoryMemo<T, TID> = RepositoryMemo(
        dataObserver = dataObserver,
        dispatchers = dispatchers,
        appScope = appScope,
        writeEvent = writeEvent,
    )
}

/**
 * A repository's in-memory copy of its table, read from the database once and kept current by
 * the repository's own writes. Readers on any thread see a consistent map; the reads that fill
 * it and the writes that change it take turns, so a row deleted while [findAll] is reading the
 * table can't be put back by it. Writes post the memo's write event, if it has one, once done.
 */
class RepositoryMemo<T : Identifiable<TID>, TID : UniqueId> internal constructor(
    private val dataObserver: DataObserver,
    private val dispatchers: DispatchersProvider,
    appScope: CoroutineScope,
    private val writeEvent: DataWriteEvent?,
) {

    private val _memo = ConcurrentHashMap<TID, T>()
    val items: Map<TID, T> = _memo

    @Volatile
    var findAllMemoized = false
        private set

    private val mutex = Mutex()

    init {
        // A backup import or "delete all data" rewrites the tables underneath this memo, through
        // the DAOs. Both announce it with AllDataChange; the memo starts over from the database.
        appScope.launch {
            dataObserver.writeEvents
                .filterIsInstance<DataWriteEvent.AllDataChange>()
                .collect { invalidate() }
        }
    }

    suspend fun findAll(
        findAllOperation: suspend () -> List<T>,
        sortMemo: Collection<T>.() -> List<T>,
    ): List<T> {
        if (findAllMemoized) return sortMemo(_memo.values)
        return mutex.withLock {
            if (!findAllMemoized) {
                val all = withContext(dispatchers.io) { findAllOperation() }
                // Replace rather than merge: a row deleted in the meantime must not survive.
                _memo.clear()
                memoize(all)
                findAllMemoized = true
            }
            sortMemo(_memo.values)
        }
    }

    suspend fun findById(
        id: TID,
        findByIdOperation: suspend (TID) -> T?
    ): T? {
        return items[id] ?: mutex.withLock {
            when {
                items[id] != null -> items[id]
                // With the whole table held, an id it hasn't is one the database hasn't either: rows
                // naming a deleted account would otherwise each ask the database for it.
                findAllMemoized -> null
                else -> withContext(dispatchers.io) { findByIdOperation(id) }?.also(::memoize)
            }
        }
    }

    suspend fun findByIds(
        ids: List<TID>,
        findByIdOperation: suspend (TID) -> T?
    ): List<T> = ids.mapNotNull { id -> findById(id, findByIdOperation) }

    suspend fun save(
        value: T,
        writeOperation: suspend (T) -> Unit,
    ) {
        mutex.withLock {
            withContext(dispatchers.io) { writeOperation(value) }
            memoize(value)
        }
        announceWrite()
    }

    suspend fun saveMany(
        values: List<T>,
        writeOperation: suspend (List<T>) -> Unit,
    ) {
        mutex.withLock {
            withContext(dispatchers.io) { writeOperation(values) }
            memoize(values)
        }
        announceWrite()
    }

    suspend fun deleteById(
        id: TID,
        deleteByIdOperation: suspend (TID) -> Unit,
    ) {
        mutex.withLock {
            _memo.remove(id)
            withContext(dispatchers.io) { deleteByIdOperation(id) }
        }
        announceWrite()
    }

    suspend fun deleteAll(
        deleteAllOperation: suspend () -> Unit,
    ) {
        mutex.withLock {
            _memo.clear()
            withContext(dispatchers.io) { deleteAllOperation() }
        }
        announceWrite()
    }

    private suspend fun announceWrite() {
        writeEvent?.let { dataObserver.post(it) }
    }

    private suspend fun invalidate() {
        mutex.withLock {
            _memo.clear()
            findAllMemoized = false
        }
    }

    private fun memoize(items: List<T>) {
        items.forEach(::memoize)
    }

    private fun memoize(item: T) {
        _memo[item.id] = item
    }
}
