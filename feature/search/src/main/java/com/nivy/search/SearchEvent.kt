package com.nivy.search

import com.nivy.data.model.Transaction

sealed interface SearchEvent {
    data class Search(val query: String) : SearchEvent
    data class SetUpcomingExpanded(val expanded: Boolean) : SearchEvent
    data class SetOverdueExpanded(val expanded: Boolean) : SearchEvent

    /** Pays a planned payment found, or gets a planned income. */
    data class PayOrGet(val transaction: Transaction) : SearchEvent
    data class Skip(val transactions: List<Transaction>) : SearchEvent
}
