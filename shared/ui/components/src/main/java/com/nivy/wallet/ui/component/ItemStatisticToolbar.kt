package com.nivy.wallet.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.design.system.colors.Transparent
import com.nivy.wallet.ui.theme.components.CircleButton
import com.nivy.wallet.ui.theme.components.DeleteButton
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton

@Composable
fun ItemStatisticToolbar(
    contrastColor: Color,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    showEditButton: Boolean = true,
    showDeleteButton: Boolean = true,
    onDelete: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val nav = navigation()
        CircleButton(
            modifier = Modifier.testTag("toolbar_close"),
            icon = R.drawable.ic_dismiss,
            borderColor = contrastColor,
            tint = contrastColor,
            backgroundColor = Transparent
        ) {
            nav.back()
        }

        Spacer(Modifier.weight(1f))

        if (showEditButton) {
            NivyOutlinedButton(
                iconStart = R.drawable.ic_edit,
                text = stringResource(R.string.edit),
                borderColor = contrastColor,
                iconTint = contrastColor,
                textColor = contrastColor,
                solidBackground = false
            ) {
                onEdit()
            }
        }

        Spacer(Modifier.width(16.dp))

        if (showDeleteButton) {
            DeleteButton {
                onDelete()
            }
        }

        Spacer(Modifier.width(24.dp))
    }
}
