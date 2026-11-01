package com.ivy.exchangerates

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.model.Theme
import com.ivy.exchangerates.component.RateItem
import com.ivy.exchangerates.data.RateUi
import com.ivy.exchangerates.modal.AddRateModal
import com.ivy.ui.R
import com.ivy.wallet.IvyWalletPreview
import com.ivy.wallet.ui.SearchInput
import com.ivy.design.utils.selectEndTextFieldValue
import com.ivy.navigation.navigation
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import com.ivy.design.system.typography.numberHeadlineMedium
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun BoxWithConstraintsScope.ExchangeRatesScreen() {
    val viewModel: ExchangeRatesViewModel = viewModel()
    val state = viewModel.uiState()

    UI(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: RatesState,
    onEvent: (RatesEvent) -> Unit,
) {
    val nav = navigation()
    var amountModalVisible by remember {
        mutableStateOf(false)
    }
    var rateToUpdate by remember {
        mutableStateOf<RateUi?>(null)
    }
    var amountModalId by remember {
        mutableStateOf(UUID.randomUUID())
    }
    val onRateClick = { rate: RateUi ->
        rateToUpdate = rate
        amountModalId = UUID.randomUUID()
        amountModalVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))
        SearchField(onSearch = { onEvent(RatesEvent.Search(it)) })
        Spacer(Modifier.height(4.dp))
        LazyColumn {
            ratesSection(text = R.string.manual_rates)
            items(items = state.manual) { rate ->
                Spacer(Modifier.height(4.dp))
                RateItem(
                    rate = rate,
                    onDelete = { onEvent(RatesEvent.RemoveOverride(rate)) },
                    onClick = { onRateClick(rate) }
                )
            }
            ratesSection(text = R.string.automatic_rates)
            items(items = state.automatic) { rate ->
                Spacer(Modifier.height(4.dp))
                RateItem(
                    rate = rate,
                    onDelete = null,
                    onClick = { onRateClick(rate) }
                )
            }
            item(key = "last_item_spacer") {
                Spacer(Modifier.height(480.dp))
            }
        }
    }

    var addRateModalVisible by remember {
        mutableStateOf(false)
    }

    ExchangeRatesBottomBar(
        onClose = { nav.back() },
        onAddRate = { addRateModalVisible = true }
    )

    AddRateModal(
        visible = addRateModalVisible,
        baseCurrency = state.baseCurrency,
        dismiss = {
            addRateModalVisible = false
        },
        onAdd = onEvent
    )

    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = "",
        initialAmount = rateToUpdate?.rate,
        dismiss = {
            amountModalVisible = false
        },
        decimalCountMax = 12,
        header = {
            rateToUpdate?.let {
                Spacer(Modifier.height(24.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = "${it.from}-${it.to}",
                    style = MaterialTheme.typography.numberHeadlineMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
        },
        onAmountChanged = { newRate ->
            rateToUpdate?.let {
                onEvent(RatesEvent.UpdateRate(rateToUpdate!!, newRate))
            }
        }
    )
}

private fun LazyListScope.ratesSection(
    @StringRes text: Int
) {
    item {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(16.dp))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchField(
    onSearch: (String) -> Unit,
) {
    var searchQueryTextFieldValue by remember {
        mutableStateOf(selectEndTextFieldValue(""))
    }

    SearchInput(
        searchQueryTextFieldValue = searchQueryTextFieldValue,
        hint = stringResource(R.string.search_currency),
        focus = false,
        showClearIcon = searchQueryTextFieldValue.text.isNotEmpty(),
        onSetSearchQueryTextField = {
            searchQueryTextFieldValue = it
            onSearch(it.text)
        }
    )
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        UI(
            state = RatesState(
                baseCurrency = "BGN",
                manual = persistentListOf(
                    RateUi("BGN", "USD", 1.85),
                    RateUi("BGN", "EUR", 1.96),
                ),
                automatic = persistentListOf(
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                )
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun ExchangeRatesScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}
