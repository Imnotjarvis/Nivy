package com.nivy.data.db.dao.fake

/** Room's upsert: an item with the same key is replaced where it is; any other is added at the end. */
internal fun <T> MutableList<T>.upsert(value: T, sameKey: (T) -> Boolean) {
    val index = indexOfFirst(sameKey)
    if (index == -1) add(value) else set(index, value)
}
