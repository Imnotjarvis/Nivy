package com.nivy.exchangerates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nivy.wallet.NivyPreview
import com.nivy.ui.R
import com.nivy.design.system.colors.Blue
import com.nivy.wallet.ui.theme.components.BackBottomBar
import com.nivy.wallet.ui.theme.components.NivyButton

@Composable
internal fun BoxWithConstraintsScope.ExchangeRatesBottomBar(
    onClose: () -> Unit,
    onAddRate: () -> Unit
) {
    BackBottomBar(onBack = onClose) {
        NivyButton(
            text = stringResource(R.string.add_manual_exchange_rate),
            iconStart = R.drawable.ic_plus
        ) {
            onAddRate()
        }
    }
}

@Preview
@Composable
private fun PreviewBottomBar() {
    NivyPreview {
        Column(
            Modifier
                .fillMaxSize()
                .background(Blue)
        ) {
        }

        ExchangeRatesBottomBar(
            onAddRate = {},
            onClose = {}
        )
    }
}
