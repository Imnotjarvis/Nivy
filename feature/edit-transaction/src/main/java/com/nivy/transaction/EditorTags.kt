package com.nivy.transaction

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.AssociationId
import com.nivy.domain.UserWrites
import com.nivy.transaction.EditTransactionViewEvent.TagEvent
import com.nivy.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.CoroutineContext

private const val SearchDebounceMillis = 500L

/**
 * The editor's tags: those the tag dialog lists, and the ids of the transaction's own. A stored
 * transaction's tags are saved as the dialog changes them, on [io], with [transactionId] the
 * transaction they're linked to. A new one's, which [stored] says it isn't yet, wait here until it's
 * saved, when [onSaved] links them: closed without saving, it leaves nothing behind. What's shown
 * changes on [scope]'s thread. [afterTagAdd] runs after the dialog adds a tag. A change the
 * database refuses shows that it failed, through [userWrites], and what's shown stays.
 */
@Stable
internal class EditorTags(
    private val transactionTags: TransactionTags,
    private val userWrites: UserWrites,
    private val scope: CoroutineScope,
    private val transactionId: () -> UUID,
    private val stored: () -> Boolean,
    private val afterTagAdd: () -> Unit,
    private val io: CoroutineContext,
) {
    var all by mutableStateOf<ImmutableList<Tag>>(persistentListOf())
        private set
    var associated by mutableStateOf<ImmutableList<TagId>>(persistentListOf())
        private set

    private var searchJob: Job? = null

    /** Lists [all] tags, and those linked to [transaction]. */
    suspend fun show(all: ImmutableList<Tag>, transaction: UUID) {
        this.all = all
        associated = transactionTags.associatedTagIds(AssociationId(transaction))
    }

    fun onEvent(event: TagEvent) {
        when (event) {
            is TagEvent.SaveTag -> create(event.name)
            is TagEvent.OnTagSelect -> associate(event.selectedTag)
            is TagEvent.OnTagDeSelect -> removeAssociation(event.selectedTag)
            is TagEvent.OnTagSearch -> search(event.query)
            is TagEvent.OnTagDelete -> delete(event.selectedTag)
            is TagEvent.OnTagEdit -> update(event.newTag)
        }
    }

    /** Links the transaction's tags to [transaction] too, before it returns: to a copy of it. */
    suspend fun linkTo(transaction: UUID) {
        if (associated.isEmpty()) return
        transactionTags.findByIds(associated).forEach {
            transactionTags.associateTag(AssociationId(transaction), it)
        }
    }

    /** Links a new transaction's tags, now that it's saved as [transaction]; a stored one's are already. */
    suspend fun onSaved(transaction: UUID) {
        if (!stored()) linkTo(transaction)
    }

    private fun create(name: String) {
        scope.launch {
            val saved = write { withContext(io) { transactionTags.saveTag(name) }?.let { all = it } }

            if (saved) afterTagAdd()
        }
    }

    private fun associate(tag: Tag) {
        if (stored()) {
            scope.launch {
                val associatedId = AssociationId(transactionId())
                write { associated = withContext(io) { transactionTags.associateTag(associatedId, tag) } }
            }
        } else if (tag.id !in associated) {
            associated = (associated + tag.id).toImmutableList()
        }
    }

    private fun removeAssociation(tag: Tag) {
        if (stored()) {
            scope.launch {
                val associatedId = AssociationId(transactionId())
                write { associated = withContext(io) { transactionTags.removeTagAssociation(associatedId, tag) } }
            }
        } else {
            associated = (associated - tag.id).toImmutableList()
        }
    }

    // Each keystroke replaces the search still waiting: only what was typed last is searched, once
    // typing pauses.
    private fun search(query: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(SearchDebounceMillis)
            all = withContext(io) { transactionTags.searchTags(query) }
        }
    }

    private fun delete(tag: Tag) {
        scope.launch {
            write {
                all = withContext(io) { transactionTags.deleteTag(tag) }
                // Deleted, it's on no transaction any more, this one included.
                associated = (associated - tag.id).toImmutableList()
            }
        }
    }

    private fun update(tag: Tag) {
        scope.launch {
            write { all = withContext(io) { transactionTags.updateTag(tag) } }
        }
    }

    private suspend fun write(change: suspend () -> Unit): Boolean =
        userWrites.run(R.string.msg_change_not_saved, change)
}
