package com.nivy.wallet.ui.theme.modal

import com.nivy.design.system.colors.deleteRed
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.ui.R
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.White
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.NivyCircleButton
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import androidx.compose.material3.MaterialTheme

@Composable
@NonRestartableComposable
fun ModalSet(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ModalCheck(
        modifier = modifier,
        label = stringResource(R.string.set),
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun ModalCheck(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ModalPositiveButton(
        modifier = modifier,
        text = label,
        iconStart = R.drawable.ic_check,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun <T> ModalAddSave(
    item: T,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    if (item != null) {
        ModalSave(
            enabled = enabled,
            onClick = onClick
        )
    } else {
        ModalAdd(
            enabled = enabled,
            onClick = onClick
        )
    }
}

@Composable
fun ModalSave(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ModalPositiveButton(
        modifier = modifier,
        text = stringResource(R.string.save),
        iconStart = R.drawable.ic_save,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun ModalAdd(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "modal_add",
    onClick: () -> Unit
) {
    ModalPositiveButton(
        modifier = modifier.testTag(testTag),
        text = stringResource(R.string.add),
        iconStart = R.drawable.ic_plus,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun ModalNegativeButton(
    text: String,
    @DrawableRes iconStart: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    NivyButton(
        text = text,
        backgroundColor = deleteRed(),
        iconStart = iconStart,
        onClick = onClick,
        enabled = enabled
    )
}

@Composable
fun ModalPositiveButton(
    text: String,
    @DrawableRes iconStart: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    NivyButton(
        modifier = modifier,
        text = text,
        backgroundColor = Green,
        iconStart = iconStart,
        onClick = onClick,
        enabled = enabled
    )
}

@Composable
fun ModalDelete(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "modal_delete",
    onClick: () -> Unit
) {
    NivyCircleButton(
        modifier = modifier
            .size(40.dp)
            .testTag(testTag),
        icon = R.drawable.ic_delete,
        backgroundColor = deleteRed(),
        enabled = enabled,
        tint = White,
        contentDescription = stringResource(R.string.delete),
        onClick = onClick
    )
}

@Composable
fun ModalTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(horizontal = 24.dp),
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )
}

@Composable
fun ModalSkip(
    onClick: () -> Unit
) {
    NivyOutlinedButton(
        text = stringResource(R.string.skip),
        iconStart = null,
        solidBackground = true
    ) {
        onClick()
    }
}
