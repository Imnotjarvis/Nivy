package com.nivy.data.di

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.pluginOrNull
import kotlinx.serialization.json.Json
import org.junit.Test

class KtorClientModuleTest {
    @Test
    fun `a build that isn't debuggable logs no responses`() {
        KtorClientModule.httpClient(Json, logBodies = false).use { client ->
            client.pluginOrNull(Logging).shouldBeNull()
        }
    }

    @Test
    fun `a debug build logs them`() {
        KtorClientModule.httpClient(Json, logBodies = true).use { client ->
            client.pluginOrNull(Logging).shouldNotBeNull()
        }
    }
}
