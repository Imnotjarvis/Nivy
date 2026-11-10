package com.nivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.design.utils.hideKeyboard
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.NivyDescriptionTextField
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.ModalAdd
import com.nivy.wallet.ui.theme.modal.ModalDelete
import com.nivy.wallet.ui.theme.modal.ModalSave
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.DescriptionModal(
    visible: Boolean,
    description: String?,
    onDescriptionChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,

    id: UUID = UUID.randomUUID(),
    dismiss: () -> Unit,
) {
    var descTextFieldValue by remember(description) {
        mutableStateOf(selectEndTextFieldValue(description))
    }
    val view = LocalView.current

    NivyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            PrimaryAction(
                initialEmpty = description.isNullOrBlank(),
                initialChanged = description != descTextFieldValue.text,
                onSave = {
                    onDescriptionChanged(descTextFieldValue.text)
                    view.hideKeyboard()
                },
                onDelete = {
                    onDescriptionChanged(null)
                    view.hideKeyboard()
                },
                dismiss = dismiss
            )
        }
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            modifier = Modifier
                .padding(start = 24.dp),
            text = stringResource(R.string.description),
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(24.dp))

        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focus.requestFocus()
        }

        NivyDescriptionTextField(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .focusRequester(focus),
            testTag = "modal_desc_input",
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = true,
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            ),
            keyboardActions = KeyboardActions(
                onAny = {
                    descTextFieldValue = descTextFieldValue.copy(
                        text = StringBuilder(descTextFieldValue.text)
                            .insert(descTextFieldValue.selection.end, "\n")
                            .toString(),
                        selection = TextRange(descTextFieldValue.selection.end + 1)
                    )
                }
            ),
            value = descTextFieldValue,
            hint = stringResource(R.string.description_text_field_hint),
        ) {
            descTextFieldValue = it
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickableNoIndication(rememberInteractionSource()) {
                    focus.requestFocus()
                }
        )
    }
}

@Preview
@Composable
private fun PreviewDescriptionModal_emptyText() {
    NivyPreview {
        DescriptionModal(
            visible = true,
            description = "",
            onDescriptionChanged = {}
        ) {
        }
    }
}

// Add for a new description, Save for a changed one, Delete for one left as it was.
@Composable
private fun PrimaryAction(
    initialEmpty: Boolean,
    initialChanged: Boolean,
    onDelete: () -> Unit,
    dismiss: () -> Unit,
    onSave: () -> Unit
) {
    when {
        initialEmpty -> ModalAdd(testTag = "modal_desc_save") {
            onSave()
            dismiss()
        }

        !initialChanged -> ModalDelete(testTag = "modal_desc_delete") {
            onDelete()
            dismiss()
        }

        else -> ModalSave(modifier = Modifier.testTag("modal_desc_save")) {
            onSave()
            dismiss()
        }
    }
}
