package com.nivy.data.repository

import arrow.core.Either
import timber.log.Timber

/**
 * The model a mapper made of a stored row, or null when it refused the row, which is then left out
 * of what the repository returns. Why is logged, so that a row missing from the lists can be traced.
 */
internal fun <T> Either<String, T>.orLogged(): T? = fold(
    ifLeft = { reason ->
        Timber.w("Left out a stored row: %s", reason)
        null
    },
    ifRight = { it },
)
