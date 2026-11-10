package com.nivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.utils.amountToDoubleOrNull
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.formatDecimals
import com.nivy.wallet.utils.formatInputAmount
import com.nivy.wallet.utils.localDecimalSeparator
import com.nivy.wallet.utils.normalizeExpression
import com.nivy.ui.R
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Red
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.ModalSet
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.notkamui.keval.Keval
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberHeadlineMedium

@Composable
internal fun BoxWithConstraintsScope.CalculatorModal(
      initialAmount: Double?,
      visible: Boolean,
      currency: String,
      decimalCountMax: Int,
      dismiss: () -> Unit,
      id: UUID = UUID.randomUUID(),
      onCalculation: (Double) -> Unit
) {
    var expression by remember(id, initialAmount) {
        mutableStateOf(initialAmount?.formatAmount(currency, decimalCountMax) ?: "")
    }

    NivyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            ModalSet(
                modifier = Modifier.testTag("calc_set")
            ) {
                val result = calculate(expression)
                if (result != null) {
                    onCalculation(result)
                    dismiss()
                }
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = stringResource(R.string.calculator))

        Spacer(Modifier.height(32.dp))

        val isEmpty = expression.isBlank()
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = if (isEmpty) stringResource(R.string.calculator_empty_expression) else expression,
            // The placeholder is words; the expression is numbers.
            style = (
                if (isEmpty) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.numberHeadlineMedium
                ).copy(
                color = if (isEmpty) Gray else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(Modifier.height(32.dp))

        AmountKeyboard(
            forCalculator = true,
            zeroRow = {
                KeypadCircleButton(
                    text = "C",
                    textColor = Red,
                    testTag = "key_C"
                ) {
                    expression = ""
                }

                KeypadCircleButton(
                    text = "(",
                    testTag = "key_("
                ) {
                    expression += "("
                }

                KeypadCircleButton(
                    text = ")",
                    testTag = "key_)"
                ) {
                    expression += ")"
                }

                KeypadCircleButton(
                    text = "÷",
                    testTag = "key_/"
                ) {
                    expression = handleOperator(expression, "÷")
                }
            },
            firstRowExtra = {
                KeypadCircleButton(
                    text = "×",
                    testTag = "key_*"
                ) {
                    expression = handleOperator(expression, "×")
                }
            },
            secondRowExtra = {
                KeypadCircleButton(
                    text = "−",
                    testTag = "key_-"
                ) {
                    expression = handleOperator(expression, "−")
                }
            },
            thirdRowExtra = {
                KeypadCircleButton(
                    text = "+",
                    testTag = "key_+"
                ) {
                    expression = handleOperator(expression, "+")
                }
            },
            fourthRowExtra = {
                KeypadCircleButton(
                    text = "=",
                    testTag = "key_="
                ) {
                    val result = calculate(expression)
                    if (result != null) {
                        expression = result.formatAmount(currency, decimalCountMax)
                    }
                }
            },

            onNumberPressed = {
                expression = formatExpression(
                    expression = expression + it,
                    currency = currency
                )
            },
            onDecimalPoint = {
                expression = formatExpression(
                    expression = expression + localDecimalSeparator(),
                    currency = currency
                )
            },
            onBackspace = {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                }
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

/** As the amount field shows it: the currency's format, or [decimalCountMax] decimals without one. */
internal fun Double.formatAmount(currency: String, decimalCountMax: Int): String =
    if (currency.isNotEmpty()) format(currency) else formatDecimals(decimalCountMax)

private fun handleOperator(expression: String, operator: String): String {
    return if (expression.isNotEmpty() && expression.last().isOperator()) {
        expression.dropLast(1) + operator
    } else {
        expression + operator
    }
}

private fun Char.isOperator(): Boolean = when (this) {
    '+', '−', '×', '÷' -> true
    else -> false
}

private fun formatExpression(expression: String, currency: String): String {
    var formattedExpression = expression

    expression
        .split("(", ")", "÷", "×", "−", "+")
        .ifEmpty {
            // handle only number expression formatting
            listOf(expression)
        }
        .forEach { part ->
            val numberPart = part.amountToDoubleOrNull()
            if (numberPart != null) {
                val formattedPart = formatInputAmount(
                    currency = currency,
                    amount = part,
                    newSymbol = ""
                )

                if (formattedPart != null) {
                    formattedExpression = formattedExpression.replace(part, formattedPart)
                }
            }
        }

    return formattedExpression
}

private fun calculate(expression: String): Double? {
    return try {
        // Keval doesn't support negative numbers, so we add a zero in front of the expression
        val expression = buildString {
            for (char in expression) {
                when (char) {
                    '÷' -> this.append('/')
                    '×' -> this.append('*')
                    '−' -> this.append('-')
                    else -> this.append(char)
                }
            }
        }
        val modifiedExpression = if (expression.startsWith("-")) "0$expression" else expression
        Keval.eval(modifiedExpression.normalizeExpression())
    } catch (ignored: Exception) {
        // The expression being typed, in a library's evaluator: whatever it throws, no result yet.
        null
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        CalculatorModal(
            visible = true,
            initialAmount = 50.23,
            currency = "BGN",
            decimalCountMax = 2,
            dismiss = { },
            onCalculation = {}
        )
    }
}
