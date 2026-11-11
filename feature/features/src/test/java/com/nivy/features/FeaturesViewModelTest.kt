package com.nivy.features

import android.content.Context
import com.nivy.domain.features.BoolFeature
import com.nivy.domain.features.FeatureGroup
import com.nivy.domain.features.Features
import com.nivy.features.FeatureItemViewState.FeatureHeaderViewState
import com.nivy.features.FeatureItemViewState.FeatureToggleViewState
import com.nivy.ui.testing.runTest
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class FeaturesViewModelTest {
    private val context = mockk<Context>()

    // In the order the app declares them: not by group.
    private val sortCategories = feature("sort_categories", FeatureGroup.Category, enabled = false)
    private val compactAccounts = feature("compact_accounts", FeatureGroup.Account, enabled = true)
    private val hideBalance = feature("hide_balance", FeatureGroup.Other, enabled = false)
    private val compactCategories = feature("compact_categories", FeatureGroup.Category, enabled = true)

    private val viewModel = FeaturesViewModel(
        features = mockk<Features> {
            every { allFeatures } returns listOf(sortCategories, compactAccounts, hideBalance, compactCategories)
        },
        context = context,
    )

    @Test
    fun `the flags are listed under their group, the groups by name`() {
        viewModel.runTest {
            featureItemViewStates shouldBe listOf(
                FeatureHeaderViewState("Account"),
                toggle("compact_accounts"),
                FeatureHeaderViewState("Category"),
                toggle("sort_categories"),
                toggle("compact_categories"),
                FeatureHeaderViewState("Other"),
                toggle("hide_balance"),
            )
        }
    }

    @Test
    fun `a toggle saves the opposite of what the flag is, and no other flag`() {
        viewModel.runTest(
            events = listOf(
                FeaturesUiEvent.ToggleFeature("compact_accounts"),
                FeaturesUiEvent.ToggleFeature("sort_categories"),
            )
        ) {}

        coVerify(exactly = 1) { compactAccounts.set(context, false) }
        coVerify(exactly = 1) { sortCategories.set(context, true) }
        coVerify(exactly = 0) { hideBalance.set(any(), any()) }
        coVerify(exactly = 0) { compactCategories.set(any(), any()) }
    }

    @Test
    fun `a toggle for a flag the app doesn't have saves nothing`() {
        viewModel.runTest(events = listOf(FeaturesUiEvent.ToggleFeature("a_flag_from_another_version"))) {}

        listOf(sortCategories, compactAccounts, hideBalance, compactCategories).forEach {
            coVerify(exactly = 0) { it.set(any(), any()) }
        }
    }

    // A flag reads its value from the device's settings file: the test stands in for it.
    private fun feature(key: String, group: FeatureGroup, enabled: Boolean) = mockk<BoolFeature>(relaxed = true) {
        every { this@mockk.key } returns key
        every { this@mockk.group } returns group
        every { name } returns "Name of $key"
        every { description } returns "What $key does"
        every { enabledFlow(context) } returns flowOf(enabled)
    }

    // The list shows a flag as off until its stored value arrives; here none does.
    private fun toggle(key: String) = FeatureToggleViewState(
        key = key,
        name = "Name of $key",
        enabled = false,
        description = "What $key does",
    )
}
