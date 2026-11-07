package com.nivy.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.base.model.AppLockDelay
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.NivyModal
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
) {
    NivyModal(
        id = ModalId,
        visible = visible,
        dismiss = dismiss,
        primaryAction = { },
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = stringResource(R.string.lock_after_leaving))

        Spacer(Modifier.height(16.dp))

        for (delay in AppLockDelay.entries) {
            val isSelected = delay == selected
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (isSelected) Nivy else Color.Transparent)
                    .clickable {
                        onSelect(delay)
                        dismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("lock_delay_${delay.name}"),
                text = delay.label(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isSelected) White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

private val ModalId = UUID.randomUUID()
