package com.nivy.wallet.ui.edit.core

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.wallet.NivyComponentPreview
import com.nivy.design.utils.isKeyboardVisible
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.ui.R
import com.nivy.wallet.domain.logic.SUGGESTIONS_LIMIT
import com.nivy.wallet.domain.logic.TitleSuggestion
import com.nivy.wallet.ui.theme.components.NivyTitleTextField
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.nivy.data.model.Category
import com.nivy.design.system.colors.categoryColor
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.shapes.PillShape
import com.nivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.nivy.wallet.ui.theme.components.typedHighlight

@Suppress("ParameterNaming")
@Composable
fun ColumnScope.Title(
    type: TransactionType,
    titleFocus: FocusRequester,
    titleTextFieldValue: TextFieldValue,
    setTitleTextFieldValue: (TextFieldValue) -> Unit,
    suggestions: ImmutableList<TitleSuggestion>,
    onTitleChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null,
    // The title done, by Enter or by a suggestion chosen, with the category the suggestion brings.
    onDone: (suggestedCategory: Category?) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    NivyTitleTextField(
        modifier = modifier
            .padding(horizontal = 32.dp)
            .focusRequester(titleFocus),
        dividerModifier = Modifier
            .padding(horizontal = 24.dp),
        value = titleTextFieldValue,
        hint = when (type) {
            TransactionType.INCOME -> stringResource(R.string.income_title)
            TransactionType.EXPENSE -> stringResource(R.string.expense_title)
            TransactionType.TRANSFER -> stringResource(R.string.transfer_title)
        },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.Sentences
        ),
        // Enter confirms the title: the keyboard closes, and the rest is still to fill in.
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onDone(null)
            }
        )
    ) {
        setTitleTextFieldValue(it)
        onTitleChanged(it.text)
    }

    val coroutineScope = rememberCoroutineScope()
    Suggestions(
        suggestions = suggestions,
        typed = titleTextFieldValue.text,
    ) { suggestion ->
        setTitleTextFieldValue(selectEndTextFieldValue(suggestion.title))
        onTitleChanged(suggestion.title)
        // A suggestion chosen is the title done, as Enter is.
        focusManager.clearFocus()

        coroutineScope.launch {
            // scroll to top for better UX
            scrollState?.animateScrollTo(0)
        }
        onDone(suggestion.category)
    }
}

@Composable
private fun Suggestions(
    suggestions: ImmutableList<TitleSuggestion>,
    typed: String,
    onClick: (TitleSuggestion) -> Unit
) {
    val keyboardVisible = isKeyboardVisible()
    if (keyboardVisible) {
        TitleSuggestionList(suggestions = suggestions, typed = typed, onClick = onClick)
    }
}

/** The titles suggested under the title, with what's [typed] in them marked, each with its category. */
@Composable
internal fun TitleSuggestionList(
    suggestions: ImmutableList<TitleSuggestion>,
    typed: String,
    onClick: (TitleSuggestion) -> Unit
) {
    for (suggestion in suggestions.take(SUGGESTIONS_LIMIT)) {
        Suggestion(suggestion = suggestion, typed = typed.trim()) {
            onClick(suggestion)
        }
    }
}

@Composable
private fun Suggestion(
    suggestion: TitleSuggestion,
    typed: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable {
                onClick()
            }
            .padding(horizontal = 24.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = typedHighlight(
                text = suggestion.title,
                start = 0.takeIf { suggestion.title.startsWith(typed, ignoreCase = true) },
                length = typed.length,
            ),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        suggestion.category?.let { category ->
            Spacer(Modifier.width(12.dp))
            Text(
                modifier = Modifier.widthIn(max = 144.dp),
                text = category.name.value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            val color = categoryColor(category.color.value)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color, PillShape),
                contentAlignment = Alignment.Center
            ) {
                ItemIconSDefaultIcon(
                    iconName = category.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_s,
                    tint = readableOn(color)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTitleWithSuggestions() {
    NivyComponentPreview {
        Column {
            Title(
                type = TransactionType.EXPENSE,
                titleFocus = remember { FocusRequester() },
                titleTextFieldValue = selectEndTextFieldValue(""),
                setTitleTextFieldValue = {},
                suggestions = persistentListOf(
                    TitleSuggestion("Tabu", null),
                    TitleSuggestion("Harem", null),
                    TitleSuggestion("Club 35", null),
                ),
                onTitleChanged = {}
            )
        }
    }
}
