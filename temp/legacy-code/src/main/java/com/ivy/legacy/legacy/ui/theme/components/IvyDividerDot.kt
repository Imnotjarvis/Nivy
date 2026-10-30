package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import androidx.compose.material3.MaterialTheme

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyDividerDot() {
    Spacer(
        modifier = Modifier
            .size(4.dp)
            .background(MaterialTheme.colorScheme.inverseSurface, CircleShape)
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IvyDividerDot()
    }
}
