package com.nivy.wallet.ui.theme.components

import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyComponentPreview

@Composable
fun NivyDividerLine(
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
fun NivyDividerLineRounded(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.clip(PillShape),
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Preview
@Composable
private fun Preview() {
    NivyComponentPreview {
        NivyDividerLine()
    }
}
