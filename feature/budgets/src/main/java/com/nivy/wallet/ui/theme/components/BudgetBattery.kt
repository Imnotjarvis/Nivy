package com.nivy.wallet.ui.theme.components

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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.design.utils.thenIf
import com.nivy.wallet.NivyComponentPreview
import com.nivy.wallet.utils.format
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.readableOn
import com.nivy.wallet.ui.theme.wallet.AmountCurrencyB2Row
import kotlin.math.abs
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberLabelMedium

/**
 * How much of a budget is spent: a bar filling in one calm color while within it, and red only once
 * it's exceeded, so that exceeded budgets stand out. Its content, [trailing] at its end too, reads
 * on what's under it, as [readableOnFill] draws it.
 */
@Composable
fun BudgetBattery(
    currency: String,
    expenses: Double,
    budget: Double,
    modifier: Modifier = Modifier,
    backgroundNotFilled: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable (tint: Color) -> Unit)? = null,
) {
    if (budget == 0.0) return
    val percentSpent = expenses / budget

    val fill = if (percentSpent > 1.0) expenseRed(backgroundNotFilled) else Nivy
    // For the background: readableOnFill redraws what the fill covers.
    val textColor = MaterialTheme.colorScheme.onSurface
    val captionTextColor = MaterialTheme.colorScheme.inverseSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundNotFilled)
            .drawBehind {
                drawRect(
                    color = fill,
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
            .readableOnFill(fraction = percentSpent, fill = fill)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        NivyIcon(
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

        if (trailing != null) {
            Spacer(Modifier.weight(1f))

            trailing(textColor)

            Spacer(Modifier.width(8.dp))
        }
    }
}

/**
 * Draws the content over the first [fraction] of the width, where a bar's [fill] covers it, in
 * the color that reads on the fill; the rest keeps its own colors, those for the bar's background.
 * A text the fill's edge cuts through reads on both sides of it.
 */
internal fun Modifier.readableOnFill(fraction: Double, fill: Color): Modifier {
    val onFill = readableOn(fill)
    val covered = fraction.coerceIn(0.0, 1.0).toFloat()
    return this
        // A layer of its own, so that the recoloring reaches the content alone.
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            // Keeps the content's shapes, in onFill.
            drawRect(
                color = onFill,
                size = size.copy(width = size.width * covered),
                blendMode = BlendMode.SrcIn,
            )
        }
}

@Preview
@Composable
private fun Preview_budget_0() {
    NivyComponentPreview {
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
    NivyComponentPreview {
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
    NivyComponentPreview {
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
    NivyComponentPreview {
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
    NivyComponentPreview {
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
    NivyComponentPreview {
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
    NivyComponentPreview {
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
    NivyComponentPreview {
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
    NivyComponentPreview {
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
    NivyComponentPreview {
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
