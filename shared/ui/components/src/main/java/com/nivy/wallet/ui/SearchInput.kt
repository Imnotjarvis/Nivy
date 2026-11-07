package com.nivy.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.NivyBasicTextField
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

@Suppress("MagicNumber")
@Composable
fun SearchInput(
    searchQueryTextFieldValue: TextFieldValue,
    hint: String,
    modifier: Modifier = Modifier,
    focus: Boolean = true,
    showClearIcon: Boolean = true,
    onSetSearchQueryTextField: (TextFieldValue) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Gray, PillShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NivyIcon(icon = R.drawable.ic_search, modifier = Modifier.weight(1f))

        val searchFocus = remember { FocusRequester() }
        // The clear icon shows at the end of the text's place, so that the lens and the text stay
        // where they are once it does.
        Box(
            modifier = Modifier.weight(5f),
            contentAlignment = Alignment.CenterEnd
        ) {
            NivyBasicTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (showClearIcon) ClearIconWidth else 0.dp)
                    .padding(vertical = 12.dp)
                    .focusRequester(searchFocus),
                value = searchQueryTextFieldValue,
                hint = hint,
                onValueChanged = {
                    onSetSearchQueryTextField(it)
                }
            )

            if (showClearIcon) {
                // No taller than the text, so that the bar keeps its height once it shows.
                NivyIcon(
                    modifier = Modifier
                        .clickable {
                            onSetSearchQueryTextField(selectEndTextFieldValue(""))
                        }
                        .padding(horizontal = 12.dp),
                    icon = R.drawable.ic_outline_clear_24
                )
            }
        }

        if (focus) {
            LaunchedEffect(Unit) {
                searchFocus.requestFocus()
            }
        }
    }
}

// The clear icon's width, with the room on its sides to tap it.
private val ClearIconWidth = 48.dp
