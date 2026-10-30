package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyDividerLine(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyDividerLineRounded(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IvyDividerLine()
    }
}
