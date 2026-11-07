package com.nivy.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.typography.numberLabelMedium
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.readableOnFill
import com.nivy.wallet.ui.theme.wallet.AmountCurrencyB1
import com.nivy.wallet.ui.theme.wallet.AmountCurrencyB2Row
import com.nivy.wallet.utils.format
import kotlin.math.abs

/**
 * The monthly savings goal, laid out as a budget is: its name and amount, then a bar of what the
 * month saved towards it, in income's green. Tapping it sets the goal.
 */
@Composable
internal fun SavingsGoalItem(
    savings: SavingsGoal,
    currency: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier = modifier.testTag("savings_goal")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoIndication(rememberInteractionSource()) { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.savings_goal),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = stringResource(R.string.savings_goal_monthly),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Gray,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (savings.goal > 0) {
                AmountCurrencyB1(
                    amount = savings.goal,
                    currency = currency,
                    amountFontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(32.dp))
        }

        Spacer(Modifier.height(12.dp))

        SavingsBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            savings = savings,
            currency = currency,
            onClick = onClick,
        )
    }
}

@Composable
private fun SavingsBar(
    savings: SavingsGoal,
    currency: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.surfaceVariant
    val fraction = if (savings.goal > 0) (savings.saved / savings.goal).coerceIn(0.0, 1.0) else 0.0
    val fill = incomeGreen(background)
    // For the background: readableOnFill redraws what the fill covers.
    val onBar = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .drawBehind {
                drawRect(color = fill, size = size.copy(width = (size.width * fraction).toFloat()))
            }
            .clickable { onClick() }
            .readableOnFill(fraction = fraction, fill = fill)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        NivyIcon(
            icon = if (savings.saved < 0) R.drawable.ic_buffer_exceeded else R.drawable.ic_buffer_ok,
            tint = onBar
        )

        Spacer(Modifier.width(16.dp))

        if (savings.goal <= 0) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(R.string.set_savings_goal),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = onBar,
                    fontWeight = FontWeight.Bold
                )
            )
            return@Row
        }

        Column {
            Text(
                text = stringResource(
                    when {
                        savings.saved < 0 -> R.string.spent_more_than_earned
                        savings.saved >= savings.goal -> R.string.savings_goal_reached
                        else -> R.string.left_to_save
                    }
                ),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = onBar,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(4.dp))

            AmountCurrencyB2Row(
                amount = when {
                    savings.saved < 0 || savings.saved >= savings.goal -> abs(savings.saved)
                    else -> savings.goal - savings.saved
                },
                currency = currency,
                textColor = onBar
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${savings.saved.format(currency)}/${savings.goal.format(currency)} $currency",
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = onBar,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}
