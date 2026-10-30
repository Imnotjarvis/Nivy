package com.ivy.legacy.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

fun <T> MutableStateFlow<T>.readOnly(): StateFlow<T> {
    return this
}

suspend fun <T> ioThread(action: suspend () -> T): T = withContext(Dispatchers.IO) {
    return@withContext action()
}

suspend fun <T> scopedIOThread(action: suspend (scope: CoroutineScope) -> T): T =
    withContext(Dispatchers.IO) {
        return@withContext action(this)
    }

suspend fun <T> computationThread(action: suspend () -> T): T = withContext(Dispatchers.Default) {
    return@withContext action()
}

suspend fun <T> uiThread(action: suspend () -> T): T = withContext(Dispatchers.Main) {
    return@withContext action()
}
