package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import com.ivy.design.system.shapes.PillShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.wallet.IvyWalletComponentPreview
import androidx.compose.material3.MaterialTheme

@Composable
internal fun IvyDividerDot() {
    Spacer(
        modifier = Modifier
            .size(4.dp)
            .background(MaterialTheme.colorScheme.inverseSurface, PillShape)
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IvyDividerDot()
    }
}
