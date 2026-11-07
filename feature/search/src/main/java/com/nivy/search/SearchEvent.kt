package com.nivy.search

sealed interface SearchEvent {
    data class Search(val query: String) : SearchEvent
}
