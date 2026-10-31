package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.design.utils.hideKeyboard
import com.ivy.wallet.utils.isNotNullOrBlank
import androidx.compose.material3.MaterialTheme

// modifier places the text and underlineModifier the line under it (callers pad them
// differently); the Column only stacks the two.
@Suppress("ParameterNaming", "ModifierNotUsedAtRoot")
@Composable
fun IvyNameTextField(
    value: TextFieldValue,
    hint: String?,
    modifier: Modifier = Modifier,
    underlineModifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        autoCorrectEnabled = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done,
        capitalization = KeyboardCapitalization.Sentences
    ),
    focusRequester: FocusRequester = remember { FocusRequester() },
    keyboardActions: KeyboardActions? = null,
    onValueChanged: (TextFieldValue) -> Unit
) {
    Column {
        val isEmpty = value.text.isBlank()

        Box(
            modifier = modifier,
            contentAlignment = Alignment.CenterStart
        ) {
            if (isEmpty && hint.isNotNullOrBlank()) {
                Text(
                    modifier = Modifier,
                    text = hint!!,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start
                    ),
                )
            }

            val view = LocalView.current
            BasicTextField(
                modifier = Modifier
                    .testTag("base_input")
                    .focusRequester(focusRequester),
                value = value,
                onValueChange = onValueChanged,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Start
                ),
                singleLine = false,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions ?: KeyboardActions(
                    onDone = {
                        hideKeyboard(view)
                    }
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        IvyDividerLineRounded(
            modifier = underlineModifier
        )
    }
}

@Preview
@Composable
private fun PreviewIvyNameTextField() {
    IvyWalletComponentPreview {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            IvyNameTextField(
                modifier = Modifier.padding(horizontal = 32.dp),
                underlineModifier = Modifier.padding(horizontal = 24.dp),
                value = TextFieldValue("Title"),
                hint = "Title",
                onValueChanged = {}
            )
        }
    }
}
