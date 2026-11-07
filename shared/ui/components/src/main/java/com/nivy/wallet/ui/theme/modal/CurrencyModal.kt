package com.nivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyPreview
import com.nivy.ui.R
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.design.system.colors.Gray
import com.nivy.wallet.ui.theme.components.CurrencyPicker
import java.util.UUID
import androidx.compose.material3.MaterialTheme

@Composable
fun BoxWithConstraintsScope.CurrencyModal(
    title: String,
    initialCurrency: NivyCurrency?,
    visible: Boolean,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    id: UUID = UUID.randomUUID(),

    onSetCurrency: (String) -> Unit
) {
    var currency by remember(id) {
        mutableStateOf(initialCurrency ?: NivyCurrency.getDefault())
    }

    NivyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            ModalSave(
                modifier = Modifier.testTag("set_currency_save")
            ) {
                onSetCurrency(currency.code)
                dismiss()
            }
        },
        includeActionsRowPadding = false,
        scrollState = null
    ) {
        var keyboardVisible by remember {
            mutableStateOf(false)
        }

        if (!keyboardVisible) {
            Spacer(Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModalTitle(text = title)

                Spacer(Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.supports_crypto),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Gray,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.width(32.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        CurrencyPicker(
            modifier = Modifier
                .weight(1f),
            initialSelectedCurrency = currency,

            includeKeyboardShownInsetSpacer = false,
            lastItemSpacer = 120.dp,
            onKeyboardShown = { visible ->
                keyboardVisible = visible
            }
        ) {
            currency = it
        }
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        CurrencyModal(
            title = "Set currency",
            initialCurrency = null,
            visible = true,
            dismiss = {}
        ) {
        }
    }
}
