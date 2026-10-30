package com.ivy.legacy.legacy.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.utils.thenIf
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.Red
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB2Row
import kotlin.math.abs
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberLabelMedium

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BudgetBattery(
    modifier: Modifier = Modifier,
    currency: String,
    expenses: Double,
    budget: Double,
    backgroundNotFilled: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
) {
    if (budget == 0.0) return
    val percentSpent = expenses / budget

    val textColor = when {
        percentSpent <= 0.30 -> {
            MaterialTheme.colorScheme.onSurface
        }

        percentSpent <= 0.50 -> {
            White
        }

        percentSpent <= 0.75 -> {
            White
        }

        else -> White
    }

    val captionTextColor = when {
        percentSpent <= 0.30 -> {
            MaterialTheme.colorScheme.inverseSurface
        }

        percentSpent <= 0.50 -> {
            White
        }

        percentSpent <= 0.75 -> {
            White
        }

        else -> White
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundNotFilled)
            .drawBehind {
                drawRect(
                    color = when {
                        percentSpent <= 0.25 -> {
                            Green
                        }

                        percentSpent <= 0.50 -> {
                            Ivy
                        }

                        percentSpent <= 0.75 -> {
                            Orange
                        }

                        else -> Red
                    },
                    size = size.copy(
                        width = (size.width * percentSpent).toFloat()
                    )
                )
            }
            .thenIf(onClick != null) {
                clickable {
                    onClick?.invoke()
                }
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        IvyIcon(
            icon = if (percentSpent > 1.0) R.drawable.ic_buffer_exceeded else R.drawable.ic_buffer_ok,
            tint = textColor
        )

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = when {
                    percentSpent <= 1 -> {
                        stringResource(R.string.left_to_spend)
                    }

                    else -> stringResource(R.string.budget_exceeded_by)
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(4.dp))

            AmountCurrencyB2Row(
                amount = abs(budget - expenses),
                currency = currency,
                textColor = textColor
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${expenses.format(currency)}/${budget.format(currency)} $currency",
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = captionTextColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}

@Preview
@Composable
private fun Preview_budget_0() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                budget = 0.0,
                expenses = 100.45,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_expenses_0() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                budget = 1000.0,
                expenses = 0.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_spent_very_low() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                expenses = 5000.0,
                budget = 100000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_25() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                expenses = 5000.0,
                budget = 20000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_50() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                expenses = 5000.0,
                budget = 10000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_75() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                expenses = 5000.0,
                budget = 7500.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_90() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                expenses = 5000.0,
                budget = 5500.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_100() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                expenses = 5000.0,
                budget = 5000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_125() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                expenses = 5000.0,
                budget = 2500.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_expenses_negative() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                expenses = -348.54,
                budget = 1000.0,
                currency = "BGN"
            )
        }
    }
}
