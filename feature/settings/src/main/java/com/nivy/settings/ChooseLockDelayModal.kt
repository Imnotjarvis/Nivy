package com.nivy.settings

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.nivy.base.model.AppLockDelay
import com.nivy.ui.R
import java.util.UUID

/** The delay as the settings show it: "Immediately", "1 minute" or "5 minutes". */
@Composable
@ReadOnlyComposable
internal fun AppLockDelay.label(): String = stringResource(
    when (this) {
        AppLockDelay.IMMEDIATELY -> R.string.lock_delay_immediately
        AppLockDelay.ONE_MINUTE -> R.string.lock_delay_one_minute
        AppLockDelay.FIVE_MINUTES -> R.string.lock_delay_five_minutes
    }
)

/** How long the app can be away from the screen before it asks to be unlocked again. */
@Composable
internal fun BoxWithConstraintsScope.ChooseLockDelayModal(
    visible: Boolean,
    selected: AppLockDelay,
    dismiss: () -> Unit,
    onSelect: (AppLockDelay) -> Unit,
) = ChooseOptionModal(
    id = ModalId,
    visible = visible,
    title = stringResource(R.string.lock_after_leaving),
    options = AppLockDelay.entries,
    selected = selected,
    label = { it.label() },
    style = MaterialTheme.typography.bodyLarge,
    tag = { "lock_delay_${it.name}" },
    dismiss = dismiss,
    onSelect = onSelect,
)

private val ModalId = UUID.randomUUID()
