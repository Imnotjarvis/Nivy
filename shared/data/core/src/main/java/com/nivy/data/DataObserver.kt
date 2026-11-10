package com.nivy.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataObserver @Inject constructor() {
    private val _writeEvents = MutableSharedFlow<DataWriteEvent>()
    val writeEvents: Flow<DataWriteEvent> = _writeEvents

    suspend fun post(event: DataWriteEvent) {
        _writeEvents.emit(event)
    }
}

/** Writes that others react to; no write announces what it wrote, as no one reads it. */
sealed interface DataWriteEvent {
    /** Accounts were saved or deleted: the accounts tab loads them again. */
    data object AccountsChanged : DataWriteEvent

    /**
     * Every table was rewritten underneath the repositories, by a backup restored, all data
     * deleted or a database transaction: memos read the database again, as the accounts tab does.
     */
    data object AllDataChange : DataWriteEvent
}
