package com.nivy.features

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.nivy.ui.ComposeViewModel
import com.nivy.domain.features.Features
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class FeaturesViewModel @Inject constructor(
    private val features: Features,
    @ApplicationContext
    private val context: Context
) : ComposeViewModel<FeaturesUiState, FeaturesUiEvent>() {

    @Composable
    override fun uiState(): FeaturesUiState {
        return FeaturesUiState(
            featureItemViewStates = getFeatures()
        )
    }

    @Composable
    fun getFeatures(): ImmutableList<FeatureItemViewState> {
        return buildList {
            features.allFeatures.groupBy { it.group }.toSortedMap(compareBy { it.name }).forEach { (group, flags) ->
                add(FeatureItemViewState.FeatureHeaderViewState(name = group.name))
                flags.forEach {
                    add(
                        FeatureItemViewState.FeatureToggleViewState(
                            key = it.key,
                            name = it.name,
                            description = it.description,
                            enabled = it.asEnabledState()
                        )
                    )
                }
            }
        }.toImmutableList()
    }

    override fun onEvent(event: FeaturesUiEvent) {
        when (event) {
            is FeaturesUiEvent.ToggleFeature -> toggleFeature(event)
        }
    }

    private fun toggleFeature(event: FeaturesUiEvent.ToggleFeature) {
        viewModelScope.launch {
            val feature = features.allFeatures.find { feature -> feature.key == event.key } ?: return@launch
            feature.set(context, !feature.enabledFlow(context).first())
        }
    }
}
