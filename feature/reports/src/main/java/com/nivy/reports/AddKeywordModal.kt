package com.nivy.reports

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.NivyTitleTextField
import com.nivy.wallet.ui.theme.modal.ModalAdd
import com.nivy.wallet.ui.theme.modal.NivyModal
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.AddKeywordModal(
    visible: Boolean,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onKeywordChanged: (String) -> Unit
) {
    var modalKeyword by remember { mutableStateOf(selectEndTextFieldValue("")) }
    // One id for the dialog's life: a new one on each keystroke registered its back handler again.
    val id = remember { UUID.randomUUID() }

    NivyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            ModalAdd {
                onKeywordChanged(modalKeyword.text)
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            modifier = Modifier.padding(start = 24.dp),
            text = stringResource(R.string.add_keyword),
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(32.dp))

        val inputFocus = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            inputFocus.requestFocus()
        }

        NivyTitleTextField(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .focusRequester(inputFocus),
            dividerModifier = Modifier.padding(horizontal = 24.dp),
            value = modalKeyword,
            hint = stringResource(R.string.keyword)
        ) {
            modalKeyword = it
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        AddKeywordModal(
            visible = true,
            dismiss = {}
        ) {
        }
    }
}
