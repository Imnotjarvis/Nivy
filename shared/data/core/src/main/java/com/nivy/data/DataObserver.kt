package com.nivy.data

import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.sync.UniqueId
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

sealed interface DataWriteEvent {
    data object AllDataChange : AccountChange, CategoryChange

    sealed interface AccountChange : DataWriteEvent
    data class SaveAccounts(val accounts: List<Account>) : AccountChange
    data class DeleteAccounts(val operation: DeleteOperation<AccountId>) : AccountChange

    sealed interface CategoryChange : DataWriteEvent
    data class SaveCategories(val categories: List<Category>) : CategoryChange
    data class DeleteCategories(val operation: DeleteOperation<CategoryId>) : CategoryChange

    sealed interface TagChange : DataWriteEvent
    data class SaveTags(val tags: List<Tag>) : TagChange
    data class DeleteTags(val operation: DeleteOperation<TagId>) : TagChange
}

sealed interface DeleteOperation<out Id : UniqueId> {
    data object All : DeleteOperation<Nothing>
    data class Just<Id : UniqueId>(val ids: List<Id>) : DeleteOperation<Id>
}
