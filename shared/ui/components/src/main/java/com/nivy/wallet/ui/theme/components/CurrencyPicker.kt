package com.nivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyComponentPreview
import com.nivy.design.utils.isKeyboardVisible
import com.nivy.design.utils.keyboardHeight
import com.nivy.design.utils.hideKeyboard
import com.nivy.ui.R
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.readableTint
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState

@Suppress("ParameterNaming")
@Composable
fun CurrencyPicker(
    initialSelectedCurrency: NivyCurrency?,
    includeKeyboardShownInsetSpacer: Boolean,
    modifier: Modifier = Modifier,

    preselectedCurrency: NivyCurrency = NivyCurrency.getDefault(),
    lastItemSpacer: Dp = 0.dp,

    onKeyboardShown: (keyboardVisible: Boolean) -> Unit = {},

    onSelectedCurrencyChanged: (NivyCurrency) -> Unit
) {
    val keyboardShown = isKeyboardVisible()
    val latestOnKeyboardShown by rememberUpdatedState(onKeyboardShown)
    LaunchedEffect(keyboardShown) {
        latestOnKeyboardShown(keyboardShown)
    }

    // Follows the keyboard as it opens and closes.
    val keyboardShownInsetDp = keyboardHeight()

    Column(
        modifier = modifier
    ) {
        var preselected by remember {
            mutableStateOf(initialSelectedCurrency == null)
        }
        var selectedCurrency by remember {
            mutableStateOf(initialSelectedCurrency ?: preselectedCurrency)
        }

        var searchTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }

        if (!keyboardShown) {
            SelectedCurrencyCard(
                currency = selectedCurrency,
                preselected = preselected
            )

            Spacer(Modifier.height(20.dp))
        }

        SearchInput(searchTextFieldValue = searchTextFieldValue) {
            searchTextFieldValue = it
        }

        Spacer(Modifier.height(20.dp))

        CurrencyList(
            searchQuery = searchTextFieldValue.text,
            selectedCurrency = selectedCurrency,
            lastItemSpacer = if (includeKeyboardShownInsetSpacer) {
                keyboardShownInsetDp + lastItemSpacer
            } else {
                lastItemSpacer
            },
        ) {
            preselected = false
            selectedCurrency = it
            onSelectedCurrencyChanged(it)
        }
    }
}

@Composable
private fun SearchInput(
    searchTextFieldValue: TextFieldValue,
    onSetSearchTextFieldValue: (TextFieldValue) -> Unit
) {
    val inputFocus = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(PillShape)
            .border(2.dp, MaterialTheme.colorScheme.inverseSurface, PillShape)
            .clickable {
                inputFocus.requestFocus()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        NivyIcon(
            modifier = Modifier.padding(vertical = 8.dp),
            icon = R.drawable.ic_search
        )

        Spacer(Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.CenterStart
        ) {
            if (searchTextFieldValue.text.isEmpty()) {
                // Hint
                Text(
                    text = stringResource(R.string.search_currency),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            val view = LocalView.current
            BasicTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .focusRequester(inputFocus)
                    .testTag("search_input"),
                value = searchTextFieldValue,
                onValueChange = {
                    onSetSearchTextFieldValue(it.copy(it.text.trim()))
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardActions = KeyboardActions(
                    onDone = {
                        hideKeyboard(view)
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
            )
        }
    }
}

@Composable
private fun SelectedCurrencyCard(
    currency: NivyCurrency,
    preselected: Boolean,
) {
    val fill = if (preselected) Green else Nivy
    // Readable on the card, also where minimal colors turn it into the text color.
    val content = readableTint(fill, White)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                color = fill,
                shape = MaterialTheme.shapes.large
            )
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        Column {
            Text(
                text = currency.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = content,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = content,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.weight(1f))

        NivyIcon(
            icon = R.drawable.ic_check,
            tint = content
        )

        Text(
            text = if (preselected) stringResource(R.string.pre_selected) else stringResource(R.string.selected_text),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = content,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
@Suppress("ParameterNaming")
private fun CurrencyList(
    searchQuery: String,
    selectedCurrency: NivyCurrency,
    lastItemSpacer: Dp,
    onCurrencySelected: (NivyCurrency) -> Unit
) {
    val currencies = NivyCurrency.getAvailable()
        .matching(searchQuery)
        .sortedBy { it.code }
        .sortedBy { it.isCrypto }

    val currenciesWithLetters = mutableListOf<Any>()

    var lastFirstLetter: String? = null
    for (currency in currencies) {
        val firstLetter = if (currency.isCrypto) stringResource(R.string.crypto) else currency.code.first().toString()
        if (firstLetter != lastFirstLetter) {
            currenciesWithLetters.add(
                LetterDivider(
                    letter = firstLetter
                )
            )
            lastFirstLetter = firstLetter
        }

        currenciesWithLetters.add(currency)
    }

    val listState = remember(searchQuery, selectedCurrency) {
        LazyListState(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0
        )
    }

    LazyColumn(
        state = listState
    ) {
        itemsIndexed(
            items = currenciesWithLetters,
            key = { _, item ->
                when (item) {
                    is NivyCurrency -> item.code
                    is LetterDivider -> "letter-${item.letter}"
                    else -> item.toString()
                }
            },
        ) { index, item ->
            when (item) {
                is NivyCurrency -> {
                    CurrencyItemCard(
                        currency = item,
                        selected = item == selectedCurrency
                    ) {
                        onCurrencySelected(item)
                    }
                }
                is LetterDivider -> {
                    LetterDividerItem(
                        spacerTop = if (index == 0) 12.dp else 32.dp,
                        letterDivider = item
                    )
                }
            }
        }

        if (lastItemSpacer.value > 0) {
            item {
                Spacer(Modifier.height(lastItemSpacer))
            }
        }
    }
}

@Composable
private fun CurrencyItemCard(
    currency: NivyCurrency,
    selected: Boolean,

    onClick: () -> Unit,
) {
    Spacer(Modifier.height(12.dp))

    // Readable on the fill, also where minimal colors turn it into the text color.
    val selectedContent = readableTint(Nivy, White)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                color = if (selected) Nivy else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .clickable {
                onClick()
            }
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        Text(
            text = currency.code,
            style = MaterialTheme.typography.titleLarge.copy(
                color = if (selected) selectedContent else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = currency.name.take(20),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (selected) selectedContent else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun LetterDividerItem(
    spacerTop: Dp,
    letterDivider: LetterDivider
) {
    if (spacerTop > 0.dp) {
        Spacer(Modifier.height(spacerTop))
    }

    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = letterDivider.letter,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    )

    Spacer(Modifier.height(8.dp))
}

@Preview
@Composable
private fun Preview() {
    NivyComponentPreview {
        CurrencyPicker(
            initialSelectedCurrency = null,
            includeKeyboardShownInsetSpacer = true
        ) {
        }
    }
}

private data class LetterDivider(
    val letter: String
)

/** The currencies whose code or name starts with [query], whatever the case; all of them while it's blank. */
internal fun List<NivyCurrency>.matching(query: String): List<NivyCurrency> {
    val start = query.trim()
    return filter {
        start.isEmpty() || it.code.startsWith(start, ignoreCase = true) || it.name.startsWith(start, ignoreCase = true)
    }
}
