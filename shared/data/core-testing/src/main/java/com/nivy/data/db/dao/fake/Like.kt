package com.nivy.data.db.dao.fake

/** SQLite's LIKE [pattern]: % for any text, _ for one character, letters in any case. */
internal fun likeRegex(pattern: String): Regex = pattern.map { char ->
    when (char) {
        '%' -> ".*"
        '_' -> "."
        else -> Regex.escape(char.toString())
    }
}.joinToString("").toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
