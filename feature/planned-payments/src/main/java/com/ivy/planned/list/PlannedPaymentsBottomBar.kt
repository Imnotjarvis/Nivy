package com.ivy.planned.list

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.wallet.IvyWalletPreview
import com.ivy.design.utils.navigationBarInset
import com.ivy.design.utils.toDensityDp
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.ActionsRow
import com.ivy.wallet.ui.theme.components.CloseButton
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import com.ivy.design.system.colors.gradientCutBackgroundTop
import androidx.compose.material3.MaterialTheme

@Composable
internal fun BoxWithConstraintsScope.PlannedPaymentsBottomBar(
    onClose: () -> Unit,
    bottomInset: Dp = navigationBarInset().toDensityDp(),
    onAdd: () -> Unit
) {
    ActionsRow(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .gradientCutBackgroundTop(MaterialTheme.colorScheme.surface, LocalDensity.current)
            .padding(bottom = bottomInset)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.width(20.dp))

        CloseButton {
            onClose()
        }

        Spacer(Modifier.weight(1f))

        IvyOutlinedButton(
            iconStart = R.drawable.ic_planned_payments,
            text = stringResource(R.string.add_payment),
            solidBackground = true
        ) {
            onAdd()
        }

        Spacer(Modifier.width(20.dp))
    }
}

@Preview
@Composable
private fun PreviewBottomBar() {
    IvyWalletPreview {
        PlannedPaymentsBottomBar(
            bottomInset = 16.dp,
            onAdd = {},
            onClose = {}
        )
    }
}
