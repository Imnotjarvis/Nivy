package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.design.utils.clickableNoIndication
import com.ivy.design.utils.hideKeyboard
import com.ivy.wallet.utils.isNotNullOrBlank
import com.ivy.design.utils.rememberInteractionSource
import androidx.compose.material3.MaterialTheme

@Suppress("ParameterNaming")
@Composable
fun IvyChecklistTextField(
    value: TextFieldValue,
    hint: String?,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    readOnly: Boolean = false,
    fontWeight: FontWeight = FontWeight.Medium,
    hintFontWeight: FontWeight = FontWeight.Medium,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    hintColor: Color = MaterialTheme.colorScheme.inverseSurface,
    textAlign: TextAlign = TextAlign.Start,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions? = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = KeyboardActions.Default,
    paddingVertical: Dp = 16.dp,
    onValueChanged: (TextFieldValue) -> Unit
) {
    val isEmpty = value.text.isBlank()

    Box(
        modifier = modifier,
        contentAlignment = when (textAlign) {
            TextAlign.Left -> Alignment.CenterStart
            TextAlign.Right -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            TextAlign.Justify -> Alignment.CenterEnd
            TextAlign.Start -> Alignment.CenterStart
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterEnd
        }
    ) {
        val inputFieldFocus = FocusRequester()

        if (isEmpty && hint.isNotNullOrBlank()) {
            Text(
                modifier = textModifier
                    .clickableNoIndication(rememberInteractionSource()) {
                        inputFieldFocus.requestFocus()
                    }
                    .padding(vertical = paddingVertical),
                text = hint!!,
                textAlign = textAlign,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = hintColor,
                    fontWeight = hintFontWeight,
                    textAlign = textAlign
                )
            )
        }

        val view = LocalView.current
        BasicTextField(
            modifier = textModifier
                .focusRequester(inputFieldFocus)
                .clickableNoIndication(rememberInteractionSource()) {
                    inputFieldFocus.requestFocus()
                }
                .padding(vertical = paddingVertical),
            value = value,
            onValueChange = onValueChanged,
            readOnly = readOnly,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontWeight = fontWeight,
                textAlign = textAlign
            ),
            singleLine = false,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions ?: KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = keyboardActions ?: KeyboardActions(
                onDone = {
                    hideKeyboard(view)
                }
            )
        )
    }
}

@Preview
@Composable
private fun PreviewIvyTextField() {
    IvyWalletComponentPreview {
        IvyChecklistTextField(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = 24.dp),
            value = TextFieldValue(),
            hint = "Hint",
            onValueChanged = {}
        )
    }
}

@Preview
@Composable
private fun PreviewIvyTextField_longText() {
    IvyWalletComponentPreview {
        IvyChecklistTextField(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = 24.dp),
            value = TextFieldValue(
                "Cur habitio favere? Sunt navises promissio grandis, primus accolaes. Yes, " +
                "there is chaos, it contacts with light."
            ),
            hint = "Hint",
            onValueChanged = {}
        )
    }
}
