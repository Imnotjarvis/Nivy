package com.nivy.paymentnotifications

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class KeepListeningState(val batteryUnrestricted: Boolean, val test: TestResult?)

sealed interface KeepListeningEvent {
    /** Back on the page, from Android's settings among others. */
    data object Shown : KeepListeningEvent
    data object Test : KeepListeningEvent
}

/** What keeps Nivy listening: Android's battery limit, and the test. */
@HiltViewModel
class KeepListeningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val test: ListeningTest,
) : ComposeViewModel<KeepListeningState, KeepListeningEvent>() {
    private var batteryUnrestricted by mutableStateOf(false)

    @Composable
    override fun uiState(): KeepListeningState {
        val result by test.result.collectAsState()
        return KeepListeningState(batteryUnrestricted = batteryUnrestricted, test = result)
    }

    override fun onEvent(event: KeepListeningEvent) {
        when (event) {
            KeepListeningEvent.Shown -> batteryUnrestricted = PhoneSettings.batteryUnrestricted(context)
            KeepListeningEvent.Test -> viewModelScope.launch { test.run() }
        }
    }
}

@Composable
fun BoxWithConstraintsScope.KeepListeningScreen() {
    val viewModel: KeepListeningViewModel = screenScopedViewModel()
    // Again on coming back from Android's settings.
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(KeepListeningEvent.Shown)
        onPauseOrDispose { }
    }
    KeepListeningUI(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

/**
 * The settings that keep phones from putting Nivy to sleep: Android's battery limit, which Nivy
 * can check, and the phone's own, which it can't; then a test that it hears notifications.
 */
@Composable
internal fun BoxWithConstraintsScope.KeepListeningUI(state: KeepListeningState, onEvent: (KeepListeningEvent) -> Unit) {
    val context = LocalContext.current
    NotificationsPage(stringResource(R.string.keep_nivy_listening)) {
        item {
            PageText(stringResource(R.string.keep_listening_intro))
            Spacer(Modifier.height(24.dp))
            Step(
                number = 1,
                title = stringResource(R.string.battery_use),
                text = stringResource(
                    if (state.batteryUnrestricted) R.string.battery_unrestricted else R.string.battery_restricted,
                ),
                done = state.batteryUnrestricted,
                action = if (state.batteryUnrestricted) null else stringResource(R.string.open_settings),
                onAction = { PhoneSettings.openBatteryUse(context) },
            )
            Spacer(Modifier.height(12.dp))
            Step(
                number = 2,
                title = stringResource(R.string.phone_settings),
                text = stringResource(R.string.phone_settings_description),
                action = stringResource(R.string.app_info),
                onAction = { PhoneSettings.openAppInfo(context) },
            )
            Spacer(Modifier.height(12.dp))
            Step(
                number = 3,
                title = stringResource(R.string.test_it),
                text = state.test?.let { testText(it) } ?: stringResource(R.string.test_it_description),
                done = state.test == TestResult.Working,
                action = if (state.test == TestResult.Waiting) null else stringResource(R.string.test_it),
                onAction = { onEvent(KeepListeningEvent.Test) },
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}
