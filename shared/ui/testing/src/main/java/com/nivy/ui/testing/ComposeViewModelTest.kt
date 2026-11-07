package com.nivy.ui.testing

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withCompositionLocals
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.paparazzi.Paparazzi
import app.cash.turbine.test
import com.nivy.ui.ComposeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule

open class ComposeViewModelTest {
    @get:Rule
    val paparazzi = Paparazzi()
}

/**
 * Runs a [ComposeViewModel] test simulation.
 * Compose runtime effects are executed [RecompositionMode.Immediate].
 * @param events pass the events that have occurred in your simulation
 * @param context for a view model whose state reads resources, such as its strings:
 * [ComposeViewModelTest]'s `paparazzi.context`
 * @param verify assert what's the expected state after all the events
 */
fun <UiState, UiEvent> ComposeViewModel<UiState, UiEvent>.runTest(
    events: List<UiEvent> = emptyList(),
    context: Context? = null,
    verify: UiState.() -> Unit
) = runComposeTest(
    present = { uiState() },
    actions = { events.forEach(::onEvent) },
    context = context,
    verify = verify,
)

/**
 * The same simulation for state that comes from any composable function, such as the uiState() of
 * a view model that isn't a [ComposeViewModel].
 * @param present produces the state
 * @param actions what happens in your simulation, such as the view model's events
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <UiState> runComposeTest(
    present: @Composable () -> UiState,
    actions: () -> Unit = {},
    context: Context? = null,
    verify: UiState.() -> Unit,
) {
    try {
        Dispatchers.setMain(Dispatchers.Unconfined)
        kotlinx.coroutines.test.runTest {
            moleculeFlow(mode = RecompositionMode.Immediate) {
                if (context == null) {
                    present()
                } else {
                    withCompositionLocals(
                        LocalContext provides context,
                        LocalConfiguration provides context.resources.configuration,
                    ) {
                        present()
                    }
                }
            }.test {
                actions()
                verify(expectMostRecentItem())
                cancel()
            }
        }
    } finally {
        Dispatchers.resetMain()
    }
}
