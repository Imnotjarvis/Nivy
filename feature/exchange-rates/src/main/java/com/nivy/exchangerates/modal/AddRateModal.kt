package com.nivy.exchangerates.modal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Orange
import com.nivy.exchangerates.RatesEvent
import com.nivy.exchangerates.data.RateUi
import com.nivy.ui.R
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.wallet.ui.theme.components.NivyNameTextField
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.ModalAdd
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.edit.AmountModal
import com.nivy.wallet.domain.data.NivyCurrency
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import com.nivy.design.system.typography.numberHeadlineMedium
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
internal fun BoxWithConstraintsScope.AddRateModal(
    visible: Boolean,
    baseCurrency: String,
    dismiss: () -> Unit,
    onAdd: (RatesEvent.AddRate) -> Unit,
) {
    var toCurrency by remember { mutableStateOf(selectEndTextFieldValue("")) }
    var amountModalVisible by remember { mutableStateOf(false) }
    var rate by remember { mutableStateOf<Double?>(null) }

    val addable = addableRate(baseCurrency = baseCurrency, currency = toCurrency.text, rate = rate)

    NivyModal(
        id = null,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            ModalAdd(enabled = addable != null) {
                addable?.let { onAdd(RatesEvent.AddRate(it)) }
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(16.dp))
        ModalTitle(text = stringResource(R.string.add_rate))
        Spacer(Modifier.height(24.dp))

        NivyNameTextField(
            modifier = Modifier.padding(horizontal = 24.dp),
            underlineModifier = Modifier.padding(horizontal = 24.dp),
            value = toCurrency,
            hint = stringResource(R.string.currency)
        ) {
            toCurrency = it
        }

        Spacer(Modifier.height(12.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    amountModalVisible = true
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            text = "$baseCurrency-${toCurrency.text} = ${rate ?: "???"}",
            style = MaterialTheme.typography.numberHeadlineMedium.copy(
                color = Orange,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
        Spacer(Modifier.height(24.dp))
    }

    AmountModal(
        id = remember { UUID.randomUUID() },
        visible = amountModalVisible,
        currency = "",
        initialAmount = rate,
        decimalCountMax = 12,
        dismiss = { amountModalVisible = false },
        onAmountChanged = {
            rate = it
        }
    )
}

/**
 * The rate the dialog would add: from [baseCurrency] to the [currency] typed, a code the app knows
 * other than the base, at a [rate] above 0; null until there's one.
 */
internal fun addableRate(baseCurrency: String, currency: String, rate: Double?): RateUi? {
    val code = currency.trim().uppercase()
    val anotherKnownCurrency = NivyCurrency.fromCode(code) != null && code != baseCurrency
    return if (anotherKnownCurrency && rate != null && rate > 0) {
        RateUi(from = baseCurrency, to = code, rate = rate)
    } else {
        null
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        AddRateModal(
            visible = true,
            baseCurrency = "USD",
            dismiss = {},
            onAdd = {},
        )
    }
}
