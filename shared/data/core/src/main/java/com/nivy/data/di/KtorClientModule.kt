package com.nivy.data.di

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KtorClientModule {
    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val REQUEST_TIMEOUT_MS = 30_000L

    @Provides
    @Singleton
    fun provideKtorClient(
        json: Json,
        @ApplicationContext context: Context,
    ): HttpClient = httpClient(
        json = json,
        logBodies = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
    )

    /**
     * The app's HTTP client. With [logBodies], every response is logged in full: only debug builds
     * plant a log tree to read it, and logging a body reads all of it a second time.
     */
    internal fun httpClient(json: Json, logBodies: Boolean): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        // The exchange-rate files are a few hundred KB; a server that stalls must not hold
        // the sync (and the coroutine behind it) open for good.
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }

        if (logBodies) {
            install(Logging) {
                level = LogLevel.BODY
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.d(message)
                    }
                }
            }
        }
    }
}
