package com.ivy.ui

import timber.log.Timber

/**
 * Times the steps of a screen's load, one after another, and logs them in one line once it's
 * [done], tagged [Tag]: where the load spends its time, in a debug build's Logcat.
 */
class LoadTimer(private val load: String) {
    private val start = System.nanoTime()
    private val steps = mutableListOf<Pair<String, Long>>()

    /** Runs [block], timed as the step [name]. */
    suspend fun <T> step(name: String, block: suspend () -> T): T {
        val begin = System.nanoTime()
        return block().also { steps += name to millisSince(begin) }
    }

    /** Logs how long the load took and each of its steps. */
    fun done() {
        Timber.tag(Tag).i(
            "%s in %d ms: %s",
            load,
            millisSince(start),
            steps.joinToString { (name, millis) -> "$name $millis ms" },
        )
    }

    private fun millisSince(start: Long): Long = (System.nanoTime() - start) / NanosPerMilli

    companion object {
        const val Tag = "NivyLoad"
        private const val NanosPerMilli = 1_000_000L
    }
}
