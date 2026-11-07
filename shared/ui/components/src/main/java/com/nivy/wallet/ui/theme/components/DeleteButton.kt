package com.nivy.wallet.ui.theme.components

import com.nivy.design.system.colors.deleteRed
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.ui.R
import com.nivy.design.system.colors.White

@Composable
fun DeleteButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NivyCircleButton(
        modifier = modifier
            .size(48.dp)
            .testTag("delete_button"),
        backgroundPadding = 6.dp,
        icon = R.drawable.ic_delete,
        backgroundColor = deleteRed(),
        enabled = true,
        tint = White,
        contentDescription = stringResource(R.string.delete),
        onClick = onClick
    )
}
