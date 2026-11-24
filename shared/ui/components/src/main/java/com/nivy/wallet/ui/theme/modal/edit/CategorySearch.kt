package com.nivy.wallet.ui.theme.modal.edit

import com.nivy.data.model.Category
import java.text.Collator

/** These categories from A to Z, as the phone's language orders names: whatever the case or accents. */
internal fun List<Category>.aToZ(): List<Category> {
    val names = Collator.getInstance()
    return sortedWith { a, b -> names.compare(a.name.value, b.name.value) }
}

/** The categories whose name, or a word in it, starts with [query], whatever the case: those whose name does first. */
internal fun List<Category>.matching(query: String): List<Category> {
    if (query.isBlank()) return this
    return mapNotNull { category -> matchStart(category.name.value, query)?.let { category to it } }
        .sortedBy { (_, start) -> start != 0 }
        .map { (category, _) -> category }
}

/** Where in [name] what's typed starts, whatever the case: at its start, or at a word's; null when nowhere. */
internal fun matchStart(name: String, query: String): Int? {
    val typed = query.trim()
    return when {
        typed.isEmpty() -> null
        name.startsWith(typed, ignoreCase = true) -> 0
        else -> Word.findAll(name).firstOrNull { it.value.startsWith(typed, ignoreCase = true) }?.range?.first
    }
}

// A name's words: its letters and digits, between anything else.
private val Word = Regex("[\\p{L}\\p{N}]+")
