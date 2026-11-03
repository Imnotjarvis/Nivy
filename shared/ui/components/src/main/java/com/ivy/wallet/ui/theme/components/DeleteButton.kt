package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.design.system.colors.White
import com.ivy.design.system.colors.Gradient
import com.ivy.design.system.colors.Palette

@Composable
fun DeleteButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IvyCircleButton(
        modifier = modifier
            .size(48.dp)
            .testTag("delete_button"),
        backgroundPadding = 6.dp,
        icon = R.drawable.ic_delete,
        backgroundGradient = Gradient.solid(Palette.Red),
        enabled = true,
        tint = White,
        contentDescription = stringResource(R.string.delete),
        onClick = onClick
    )
}
