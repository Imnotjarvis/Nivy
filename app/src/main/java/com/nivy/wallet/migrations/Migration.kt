package com.nivy.wallet.migrations

interface Migration {
    val key: String

    suspend fun migrate()
}
