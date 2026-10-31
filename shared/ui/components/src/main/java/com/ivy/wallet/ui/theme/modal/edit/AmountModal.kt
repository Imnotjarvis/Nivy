package com.ivy.wallet.ui.theme.modal.edit

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.domain.di.FeaturesEntryPoint
import com.ivy.wallet.IvyWalletPreview
import com.ivy.wallet.utils.amountToDouble
import com.ivy.wallet.utils.amountToDoubleOrNull
import com.ivy.wallet.utils.format
import com.ivy.wallet.utils.formatInputAmount
import com.ivy.wallet.utils.formatInt
import com.ivy.design.utils.hideKeyboard
import com.ivy.wallet.utils.localDecimalSeparator
import com.ivy.ui.R
import com.ivy.design.system.colors.Red
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalPositiveButton
import com.ivy.wallet.ui.theme.modal.ModalPreviewActionRowHeight
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber
import java.util.UUID
import kotlin.math.truncate
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberHeadlineMedium
import androidx.compose.runtime.LaunchedEffect

@SuppressLint("ComposeModifierMissing")
@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.AmountModal(
    id: UUID,
    visible: Boolean,
    currency: String,
    initialAmount: Double?,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showPlusMinus: Boolean = false,
    decimalCountMax: Int = 2,
    header: (@Composable () -> Unit)? = null,
    amountSpacerTop: Dp = 64.dp,
    onAmountChanged: (Double) -> Unit,
) {
    var amount by remember(id) {
        mutableStateOf(
            if (currency.isNotEmpty()) {
                initialAmount?.takeIf { it != 0.0 }?.format(currency)
                    ?: ""
            } else {
                initialAmount?.takeIf { it != 0.0 }?.format(decimalCountMax)
                    ?: ""
            }
        )
    }

    var calculatorModalVisible by remember(id) {
        mutableStateOf(false)
    }

    IvyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            IvyIcon(
                modifier = circleButtonModifier(
                    size = 52.dp,
                    onClick = {
                        calculatorModalVisible = true
                    }
                )
                    .testTag("btn_calculator")
                    .padding(all = 4.dp),
                icon = R.drawable.ic_custom_calculator_m,
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.width(16.dp))

            ModalPositiveButton(
                text = stringResource(R.string.enter),
                iconStart = R.drawable.ic_check
            ) {
                try {
                    onAmountChanged(amount.amountToDouble())
                    dismiss()
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        },
        secondaryActions = {
            if (showPlusMinus) {
                Row {
                    Spacer(modifier = Modifier.width(24.dp))
                    KeypadCircleButton(
                        text = "+/-",
                        testTag = "plus_minus",
                        fontSize = 22.sp,
                        btnSize = 52.dp,
                        onClick = {
                            when {
                                amount.firstOrNull() == '-' -> {
                                    amount = amount.drop(1)
                                }

                                amount.isNotEmpty() -> {
                                    amount = "-$amount"
                                }
                            }
                        }
                    )
                }
            }
        }
    ) {
        header?.invoke()

        Spacer(Modifier.height(amountSpacerTop))

        val rootView = LocalView.current
        LaunchedEffect(Unit) {
            hideKeyboard(rootView)
        }

        AmountCurrency(
            amount = amount,
            currency = currency
        )

        Spacer(Modifier.height(10.dp))

        AmountInput(
            currency = currency,
            decimalCountMax = decimalCountMax,
            amount = amount
        ) {
            amount = it
        }

        Spacer(Modifier.height(24.dp))
    }

    CalculatorModal(
        visible = calculatorModalVisible,
        initialAmount = amount.amountToDoubleOrNull(),
        currency = currency,
        dismiss = {
            calculatorModalVisible = false
        },
        onCalculation = {
            amount = if (currency.isNotEmpty()) it.format(currency) else it.format(decimalCountMax)
        }
    )
}

@SuppressLint("ComposeModifierMissing")
@Composable
private fun AmountCurrency(
    amount: String,
    currency: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = amount.ifBlank { "0" },
            style = MaterialTheme.typography.numberHeadlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = currency,
            style = MaterialTheme.typography.numberHeadlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun AmountInput(
    currency: String,
    amount: String,
    decimalCountMax: Int = 2,
    setAmount: (String) -> Unit,
) {
    var firstInput by remember { mutableStateOf(true) }

    AmountKeyboard(
        horizontalPadding = 40.dp,
        forCalculator = false,
        onNumberPressed = {
            if (firstInput) {
                setAmount(it)
                firstInput = false
            } else {
                val formattedAmount = formatInputAmount(
                    currency = currency,
                    amount = amount,
                    newSymbol = it,
                    decimalCountMax = decimalCountMax
                )
                if (formattedAmount != null) {
                    setAmount(formattedAmount)
                }
            }
        },
        onDecimalPoint = {
            if (firstInput) {
                setAmount("0${localDecimalSeparator()}")
                firstInput = false
            } else {
                val newlyEnteredString = if (amount.isEmpty()) {
                    "0${localDecimalSeparator()}"
                } else {
                    "$amount${localDecimalSeparator()}"
                }
                if (newlyEnteredString.amountToDoubleOrNull() != null) {
                    setAmount(newlyEnteredString)
                }
            }
        },
        onBackspace = {
            if (firstInput) {
                setAmount("")
                firstInput = false
            } else {
                if (amount.isNotEmpty()) {
                    val formattedNumber = formatNumber(amount.dropLast(1))
                    setAmount(formattedNumber ?: "")
                }
            }
        }
    )
}

private fun formatNumber(number: String): String? {
    val decimalPartString = number
        .split(localDecimalSeparator())
        .getOrNull(1)
    val newDecimalCount = decimalPartString?.length ?: 0

    val amountDouble = number.amountToDoubleOrNull()

    if (newDecimalCount <= 2 && amountDouble != null) {
        val intPart = truncate(amountDouble).toInt()
        val decimalFormatted = if (decimalPartString != null) {
            "${localDecimalSeparator()}$decimalPartString"
        } else {
            ""
        }

        return formatInt(intPart) + decimalFormatted
    }

    return null
}

@SuppressLint(
    "ComposeContentEmitterReturningValues",
    "ComposeMultipleContentEmitters",
    "ComposeModifierMissing",
)
@Suppress("ParameterNaming")
@Composable
internal fun AmountKeyboard(
    forCalculator: Boolean,
    onNumberPressed: (String) -> Unit,
    onDecimalPoint: () -> Unit,
    horizontalPadding: Dp = 0.dp,
    zeroRow: (@Composable RowScope.() -> Unit)? = null,
    firstRowExtra: (@Composable RowScope.() -> Unit)? = null,
    secondRowExtra: (@Composable RowScope.() -> Unit)? = null,
    thirdRowExtra: (@Composable RowScope.() -> Unit)? = null,
    fourthRowExtra: (@Composable RowScope.() -> Unit)? = null,
    onBackspace: () -> Unit,
) {
    /**
     * Retrieve `features` via EntryPointAccessors. `isStandardLayout` is stable and
     * only changes via settings, so there isn't unnecessary recompositions.
     * `isStandardLayout` doesn't change while the keyboard is shown.
     * `AmountKeyboard` is self-contained, making it easy to reuse without passing parameters.
     * Layout changes are rare, so recomposition has minimal impact on performance.
     **/

    val context = LocalContext.current
    val features = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FeaturesEntryPoint::class.java
        ).getFeatures()
    }
    val isStandardLayout = features.standardKeypadLayout.asEnabledState()

    // Decide the order of the numbers based on the keypad layout
    val countButtonValues = if (isStandardLayout) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )
    } else { // Calculator like numeric keypad layout
        listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
        )
    }

    if (zeroRow != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            zeroRow.invoke(this)
        }

        Spacer(Modifier.height(8.dp))
    }

    // Loop through the rows to create CircleNumberButtons
    countButtonValues.forEachIndexed { rowIndex, rowNumbers ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            rowNumbers.forEach { num ->
                CircleNumberButton(
                    forCalculator = forCalculator,
                    value = num,
                    onNumberPressed = onNumberPressed
                )
            }

            when (rowIndex) {
                0 -> firstRowExtra?.invoke(this)
                1 -> secondRowExtra?.invoke(this)
                2 -> thirdRowExtra?.invoke(this)
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        KeypadCircleButton(
            text = localDecimalSeparator(),
            testTag = if (forCalculator) {
                "calc_key_decimal_separator"
            } else {
                "key_decimal_separator"
            }
        ) {
            onDecimalPoint()
        }

        CircleNumberButton(
            forCalculator = forCalculator,
            value = "0",
            onNumberPressed = onNumberPressed
        )

        IvyIcon(
            modifier = circleButtonModifier(onClick = onBackspace)
                .padding(all = 24.dp)
                .testTag("key_del"),
            icon = R.drawable.ic_backspace,
            tint = Red
        )

        fourthRowExtra?.invoke(this)
    }
}

@Composable
@Suppress("ParameterNaming")
fun CircleNumberButton(
    forCalculator: Boolean,
    value: String,
    onNumberPressed: (String) -> Unit,
) {
    KeypadCircleButton(
        text = value,
        testTag = if (forCalculator) {
            "calc_key_$value"
        } else {
            "key_$value"
        },
        onClick = {
            onNumberPressed(value)
        }
    )
}

@SuppressLint("ComposeModifierMissing")
@Composable
internal fun KeypadCircleButton(
    text: String,
    testTag: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 32.sp,
    btnSize: Dp = 80.dp,
    onClick: () -> Unit,
) {
    Text(
        modifier = circleButtonModifier(size = btnSize, onClick = onClick)
            .testTag(testTag),
        text = text,
        fontSize = fontSize,
        style = MaterialTheme.typography.numberHeadlineMedium.copy(
            color = textColor,
            fontWeight = FontWeight.Bold
        ).copy(
            textAlign = TextAlign.Center
        )
    )
}

@SuppressLint("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@Composable
@ReadOnlyComposable
private fun circleButtonModifier(
    size: Dp = 80.dp,
    onClick: () -> Unit,
): Modifier {
    return Modifier
        .size(size)
        .clip(CircleShape)
        .clickable(
            onClick = onClick
        )
        .background(MaterialTheme.colorScheme.surface, CircleShape)
        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        .wrapContentHeight()
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        BoxWithConstraints(
            modifier = Modifier.padding(bottom = ModalPreviewActionRowHeight)
        ) {
            AmountModal(
                id = UUID.randomUUID(),
                visible = true,
                currency = "BGN",
                initialAmount = null,
                dismiss = { }
            ) {
            }
        }
    }
}
