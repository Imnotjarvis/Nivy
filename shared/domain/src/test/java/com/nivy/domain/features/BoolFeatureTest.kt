package com.nivy.domain.features

import androidx.datastore.preferences.core.preferencesOf
import com.nivy.data.datastore.DatastoreKeys
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BoolFeatureTest {
    private fun compact(defaultValue: Boolean) = BoolFeature(
        key = "compact",
        group = FeatureGroup.Other,
        name = "Compact",
        description = "A compact layout",
        defaultValue = defaultValue,
    )

    private fun stored(key: String, enabled: Boolean) =
        flowOf(preferencesOf(DatastoreKeys.nivyFeature(key) to enabled))

    @Test
    fun `before anything is read, a screen starts from the default`() {
        compact(defaultValue = false).initialValue shouldBe false
    }

    @Test
    fun `once the stored value is read, screens opened later start from it`() = runTest {
        val feature = compact(defaultValue = false)

        feature.enabledFlow(stored("compact", enabled = true)).first()

        feature.initialValue shouldBe true
    }

    @Test
    fun `a feature never stored reads as its default`() = runTest {
        val feature = compact(defaultValue = true)

        feature.enabledFlow(flowOf(preferencesOf())).first() shouldBe true
        feature.initialValue shouldBe true
    }
}
