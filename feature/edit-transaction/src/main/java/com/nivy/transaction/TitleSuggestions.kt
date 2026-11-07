package com.nivy.transaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nivy.domain.features.Features
import com.nivy.wallet.domain.logic.SmartTitleSuggestionsLogic
import com.nivy.wallet.utils.ioThread
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/** Titles of earlier transactions, offered under the title as it's typed. */
@Stable
internal class TitleSuggestions(
    private val smartTitleSuggestionsLogic: SmartTitleSuggestionsLogic,
    private val features: Features,
    private val scope: CoroutineScope,
) {
    private var suggestions by mutableStateOf(persistentSetOf<String>())
    private var job: Job? = null

    /** The suggestions, or none while they're turned off in the settings. */
    @Composable
    fun shown(): ImmutableSet<String> =
        if (features.showTitleSuggestions.asEnabledState()) {
            suggestions
        } else {
            persistentSetOf()
        }

    /** Looks them up again, for [title] in the category [categoryId] on the account [accountId]. */
    fun update(title: String?, categoryId: UUID?, accountId: UUID?) {
        // Each keystroke asks again: an older, slower lookup must not overwrite a newer one.
        job?.cancel()
        job = scope.launch {
            suggestions = ioThread {
                smartTitleSuggestionsLogic.suggest(
                    title = title,
                    categoryId = categoryId,
                    accountId = accountId
                )
            }.toPersistentSet()
        }
    }
}
