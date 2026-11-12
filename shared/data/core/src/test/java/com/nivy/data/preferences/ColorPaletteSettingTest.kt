package com.nivy.data.preferences

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nivy.base.model.ColorPalette
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ColorPaletteSettingTest {
    private val store = InMemoryPreferencesDataStore()
    private val preferences = AppPreferences(store)

    @Test
    fun `the app's own colors are Nivy's until another palette is chosen`() = runTest {
        preferences.colorPalette.first() shouldBe ColorPalette.NIVY

        preferences.setColorPalette(ColorPalette.OCEAN)

        preferences.colorPalette.first() shouldBe ColorPalette.OCEAN
    }

    @Test
    fun `a palette this version doesn't have is Nivy's`() = runTest {
        // given one a later version might add, or one an earlier one had
        store.edit { it[stringPreferencesKey("color_palette")] = "FOREST" }

        // then
        preferences.colorPalette.first() shouldBe ColorPalette.NIVY
    }
}
