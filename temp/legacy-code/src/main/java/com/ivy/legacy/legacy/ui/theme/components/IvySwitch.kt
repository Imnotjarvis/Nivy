package com.ivy.wallet.ui.theme.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.White
import com.ivy.legacy.IvyWalletComponentPreview

// A Material3 switch in Ivy's colors: green when on, a gray outline when off.
@Composable
fun IvySwitch(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onEnabledChange: (checked: Boolean) -> Unit
) {
    Switch(
        modifier = modifier,
        checked = enabled,
        onCheckedChange = onEnabledChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = White,
            checkedTrackColor = Green,
            checkedBorderColor = Green,
            uncheckedThumbColor = Gray,
            uncheckedTrackColor = MaterialTheme.colorScheme.surface,
            uncheckedBorderColor = Gray,
        )
    )
}

@Preview
@Composable
private fun PreviewIvySwitch() {
    IvyWalletComponentPreview {
        var enabled by remember {
            mutableStateOf(false)
        }

        IvySwitch(enabled = enabled) {
            enabled = it
        }
    }
}
