package com.ivy.wallet.ui.component.transaction

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Composable
fun TransactionsDividerLine(
    modifier: Modifier = Modifier,
    paddingHorizontal: Dp = 24.dp
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = paddingHorizontal),
        color = MaterialTheme.colorScheme.surfaceVariant,
        thickness = 2.dp
    )
}
