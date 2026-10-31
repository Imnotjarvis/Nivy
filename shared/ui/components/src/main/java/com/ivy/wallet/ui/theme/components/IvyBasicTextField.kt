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
import com.ivy.design.utils.selectEndTextFieldValue
import androidx.compose.material3.MaterialTheme

@Suppress("ParameterNaming")
@Composable
fun IvyBasicTextField(
    value: TextFieldValue,
    hint: String?,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        autoCorrectEnabled = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done,
        capitalization = KeyboardCapitalization.Sentences
    ),
    keyboardActions: KeyboardActions? = null,
    onValueChanged: (TextFieldValue) -> Unit
) {
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
                .testTag("base_input"),
            value = value,
            onValueChange = onValueChanged,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold,
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
}

@Preview
@Composable
private fun Preview_Hint() {
    IvyWalletComponentPreview {
        IvyBasicTextField(
            value = selectEndTextFieldValue(""),
            hint = "Search transactions",
            onValueChanged = {}
        )
    }
}

@Preview
@Composable
private fun Preview_Filled() {
    IvyWalletComponentPreview {
        IvyBasicTextField(
            value = selectEndTextFieldValue("sfds"),
            hint = "Okay",
            onValueChanged = {}
        )
    }
}
