package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Box
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
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.design.utils.hideKeyboard
import com.ivy.wallet.utils.isNotNullOrBlank
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge

@Suppress("ParameterNaming")
@Composable
fun IvyNumberTextField(
    value: TextFieldValue,
    hint: String?,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    hintColor: Color = Color.Gray,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions? = null,
    onValueChanged: (TextFieldValue) -> Unit
) {
    val isEmpty = value.text.isBlank()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isEmpty && hint.isNotNullOrBlank()) {
            Text(
                modifier = textModifier,
                text = hint!!,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = hintColor,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Center
                )
            )
        }

        val view = LocalView.current
        BasicTextField(
            modifier = textModifier
                .testTag("base_number_input"),
            value = value,
            onValueChange = onValueChanged,
            textStyle = MaterialTheme.typography.numberBodyLarge.copy(
                color = textColor,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions ?: KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number,
                autoCorrectEnabled = false
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
private fun Preview() {
    IvyWalletComponentPreview {
        IvyNumberTextField(
            value = TextFieldValue(),
            hint = "0"
        ) {
        }
    }
}
