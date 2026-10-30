package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview

@Composable
fun IvyDividerLine(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

// The same line with rounded ends.
@Composable
fun IvyDividerLineRounded(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.clip(CircleShape),
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IvyDividerLine()
    }
}
