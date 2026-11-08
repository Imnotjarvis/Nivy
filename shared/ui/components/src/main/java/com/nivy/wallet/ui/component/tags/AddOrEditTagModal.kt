package com.nivy.wallet.ui.component.tags

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import arrow.core.raise.either
import com.nivy.data.model.Tag
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.TagId
import com.nivy.wallet.ui.theme.components.DeleteButton
import com.nivy.wallet.ui.theme.components.NivyTitleTextField
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.ModalPositiveButton
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.ui.R

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.AddOrEditTagModal(
    id: TagId,
    modifier: Modifier = Modifier,
    visible: Boolean = false,
    initialTag: Tag? = null,
    onTagAdd: (String) -> Unit = {},
    onTagEdit: (oldTag: Tag, newTag: Tag) -> Unit = { _, _ -> },
    onTagDelete: (Tag) -> Unit = {},
    onDismiss: () -> Unit
) {
    val titleFocus = remember { FocusRequester() }

    var titleTextFieldValue by remember(id) {
        mutableStateOf(
            TextFieldValue(
                initialTag?.name?.value ?: "",
                selection = TextRange(initialTag?.name?.value?.length ?: 0)
            )
        )
    }

    var filename by remember(id) {
        mutableStateOf(initialTag?.name?.value ?: "")
    }

    NivyModal(
        modifier = modifier,
        id = id.value,
        visible = visible,
        dismiss = onDismiss,
        primaryAction = {
            ModalPositiveButton(
                onClick = {
                    if (initialTag != null) {
                        val updatedTag = either {
                            initialTag.copy(
                                name = NotBlankTrimmedString.from(filename).bind()
                            )
                        }.getOrNull()

                        if (updatedTag != null) {
                            onTagEdit(initialTag, updatedTag)
                        }
                    } else {
                        onTagAdd(filename)
                    }
                    onDismiss()
                },
                text = stringResource(R.string.done),
                iconStart = R.drawable.ic_custom_document_s,
                enabled = filename.isNotEmpty()
            )
        }
    ) {
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .padding(start = 0.dp, end = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalTitle(text = if (initialTag == null) "Tag Name" else "Edit Tag Name")

            Spacer(modifier = Modifier.weight(1f))

            if (initialTag != null) {
                DeleteButton(
                    onClick = {
                        onTagDelete(initialTag)
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        NivyTitleTextField(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .focusRequester(titleFocus),
            dividerModifier = Modifier
                .padding(horizontal = 24.dp),
            value = titleTextFieldValue,
            hint = stringResource(R.string.enter_tag_name),
            // Enter confirms the name and closes the keyboard, as the field does by default.
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Sentences
            ),
        ) {
            titleTextFieldValue = it
            filename = it.text
        }

        LaunchedEffect(titleFocus) {
            titleFocus.requestFocus()
        }
    }
}
