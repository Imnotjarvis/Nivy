package com.ivy.wallet.ui.theme.modal

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.design.system.colors.GradientGreen
import com.ivy.design.system.colors.GradientRed
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import androidx.compose.material3.MaterialTheme

@Composable
internal fun ModalDynamicPrimaryAction(
    initialEmpty: Boolean,
    initialChanged: Boolean,

    onDelete: () -> Unit,
    dismiss: () -> Unit,

    testTagSave: String = "tag_save",
    testTagDelete: String = "tag_delete",
    onSave: () -> Unit
) {
    when {
        initialEmpty -> {
            ModalAdd(
                testTag = testTagSave
            ) {
                onSave()
                dismiss()
            }
        }
        else -> {
            if (!initialChanged) {
                ModalDelete(
                    testTag = testTagDelete
                ) {
                    onDelete()
                    dismiss()
                }
            } else {
                ModalSave(
                    modifier = Modifier.testTag(testTagSave)
                ) {
                    onSave()
                    dismiss()
                }
            }
        }
    }
}

@Composable
fun ModalSet(
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.set),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ModalCheck(
        modifier = modifier,
        label = label,
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
    IvyButton(
        text = text,
        backgroundGradient = GradientRed,
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
    IvyButton(
        modifier = modifier,
        text = text,
        backgroundGradient = GradientGreen,
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
    IvyCircleButton(
        modifier = modifier
            .size(40.dp)
            .testTag(testTag),
        icon = R.drawable.ic_delete,
        backgroundGradient = GradientRed,
        enabled = enabled,
        tint = White,
        onClick = onClick
    )
}

@Composable
fun ModalTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(horizontal = 32.dp),
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )
}

@Composable
fun ModalSkip(
    text: String = stringResource(R.string.skip),
    onClick: () -> Unit
) {
    IvyOutlinedButton(
        text = text,
        iconStart = null,
        solidBackground = true
    ) {
        onClick()
    }
}
