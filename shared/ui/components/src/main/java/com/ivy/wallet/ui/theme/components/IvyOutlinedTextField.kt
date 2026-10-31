package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.wallet.utils.isNotNullOrBlank
import com.ivy.design.utils.thenIf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme

@Suppress("ParameterNaming")
@Composable
fun IvyOutlinedTextField(
    value: TextFieldValue,
    hint: String?,
    modifier: Modifier = Modifier,
    hintColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    emptyBorderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    cursorColor: Color = MaterialTheme.colorScheme.onSurface,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    validateInput: (TextFieldValue) -> Boolean = { it.text.isNotNullOrBlank() },
    onValueChanged: (TextFieldValue) -> Unit
) {
    val isEmpty = value.text.isBlank()
    val rFull = CircleShape
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (isEmpty) emptyBorderColor else backgroundColor,
                shape = CircleShape
            )
            .thenIf(validateInput(value)) {
                background(backgroundColor.copy(alpha = 0.1f), rFull)
            },
        contentAlignment = Alignment.Center
    ) {
        val inputFieldFocus = FocusRequester()

        if (isEmpty && hint.isNotNullOrBlank()) {
            Text(
                modifier = Modifier
                    .clickable {
                        inputFieldFocus.requestFocus()
                    }
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                text = hint!!,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = hintColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        BasicTextField(
            modifier = Modifier
                .focusRequester(inputFieldFocus)
                .clickable {
                    inputFieldFocus.requestFocus()
                }
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 24.dp),
            value = value,
            onValueChange = onValueChanged,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            cursorBrush = SolidColor(cursorColor),
            visualTransformation = visualTransformation,
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions
        )
    }
}

@Preview
@Composable
private fun PreviewOutlineTextField() {
    IvyWalletComponentPreview {
        IvyOutlinedTextField(
            modifier = Modifier.padding(horizontal = 24.dp),
            value = TextFieldValue(),
            hint = "Hint",
            onValueChanged = {}
        )
    }
}
