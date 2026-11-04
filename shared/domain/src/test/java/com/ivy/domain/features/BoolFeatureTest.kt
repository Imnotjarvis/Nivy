package com.ivy.domain.features

import androidx.datastore.preferences.core.preferencesOf
import com.ivy.data.datastore.DatastoreKeys
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BoolFeatureTest {
    private fun stored(key: String, enabled: Boolean) =
        flowOf(preferencesOf(DatastoreKeys.ivyFeature(key) to enabled))

    @Test
    fun `before anything is read, a screen starts from the default`() {
        BoolFeature(key = "compact", defaultValue = false).initialValue shouldBe false
    }

    @Test
    fun `once the stored value is read, screens opened later start from it`() = runTest {
        val feature = BoolFeature(key = "compact", defaultValue = false)

        feature.enabledFlow(stored("compact", enabled = true)).first()

        feature.initialValue shouldBe true
    }

    @Test
    fun `a feature never stored reads as its default`() = runTest {
        val feature = BoolFeature(key = "compact", defaultValue = true)

        feature.enabledFlow(flowOf(preferencesOf())).first() shouldBe true
        feature.initialValue shouldBe true
    }
}
