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
import com.ivy.design.system.typography.numberBodyLarge

@Suppress("ParameterNaming")
@Composable
fun IvyDescriptionTextField(
    value: TextFieldValue,
    hint: String?,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    testTag: String = "desc_input",
    fontWeight: FontWeight = FontWeight.Medium,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    hintColor: Color = MaterialTheme.colorScheme.inverseSurface,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions? = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = KeyboardActions.Default,
    onValueChanged: (TextFieldValue) -> Unit
) {
    val isEmpty = value.text.isBlank()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        if (isEmpty && hint.isNotNullOrBlank()) {
            Text(
                modifier = textModifier,
                text = hint!!,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = hintColor,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Start
                )
            )
        }

        val view = LocalView.current
        BasicTextField(
            modifier = textModifier.testTag(testTag),
            value = value,
            onValueChange = onValueChanged,
            textStyle = MaterialTheme.typography.numberBodyLarge.copy(
                color = textColor,
                fontWeight = fontWeight,
                textAlign = TextAlign.Start
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
        IvyDescriptionTextField(
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
        IvyDescriptionTextField(
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
