package com.nivy.wallet.ui.theme.components

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
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.readableTint
import com.nivy.wallet.NivyComponentPreview

// A Material3 switch in Nivy's colors: green when on, a gray outline when off.
@Composable
fun NivySwitch(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onEnabledChange: (checked: Boolean) -> Unit
) {
    Switch(
        modifier = modifier,
        checked = enabled,
        onCheckedChange = onEnabledChange,
        colors = SwitchDefaults.colors(
            // Readable on the track, also where minimal colors turn it into the text color.
            checkedThumbColor = readableTint(Green, White),
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
private fun PreviewNivySwitch() {
    NivyComponentPreview {
        var enabled by remember {
            mutableStateOf(false)
        }

        NivySwitch(enabled = enabled) {
            enabled = it
        }
    }
}
