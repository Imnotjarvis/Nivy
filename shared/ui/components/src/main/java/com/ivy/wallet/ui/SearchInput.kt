package com.ivy.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.ivy.design.system.colors.Gray
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.design.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyBasicTextField
import androidx.compose.foundation.shape.CircleShape
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
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Gray, CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IvyIcon(icon = R.drawable.ic_search, modifier = Modifier.weight(1f))

        val searchFocus = remember { FocusRequester() }
        IvyBasicTextField(
            modifier = Modifier
                .weight(5f)
                .padding(vertical = 12.dp)
                .focusRequester(searchFocus),
            value = searchQueryTextFieldValue,
            hint = hint,
            onValueChanged = {
                onSetSearchQueryTextField(it)
            }
        )

        if (focus) {
            LaunchedEffect(Unit) {
                searchFocus.requestFocus()
            }
        }

        if (showClearIcon) {
            IvyIcon(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onSetSearchQueryTextField(selectEndTextFieldValue(""))
                    },
                icon = R.drawable.ic_outline_clear_24
            )
        }
    }
}
