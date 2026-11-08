package com.nivy.wallet.ui.component.transaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyComponentPreview
import com.nivy.design.utils.clickableNoIndication
import com.nivy.wallet.utils.format
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.wallet.utils.springBounce
import com.nivy.ui.R
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.shown
import com.nivy.wallet.ui.theme.components.NivyDividerDot
import com.nivy.wallet.ui.theme.components.NivyIcon
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberLabelMedium
import com.nivy.design.system.colors.Palette

@Composable
fun SectionDivider(
    expanded: Boolean,
    title: String,
    titleColor: Color,
    baseCurrency: String,
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier,

    showIncomeExpenseRow: Boolean = true,

    setExpanded: (Boolean) -> Unit
) {
    Spacer(Modifier.height(24.dp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickableNoIndication(rememberInteractionSource()) {
                setExpanded(!expanded)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val expandIconRotation by animateFloatAsState(
            targetValue = if (expanded) -180f else 0f,
            animationSpec = springBounce()
        )

        Spacer(Modifier.width(24.dp))

        Column {
            Text(
                modifier = Modifier.testTag("upcoming_title"),
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = titleColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (showIncomeExpenseRow) {
                Spacer(Modifier.height(4.dp))

                SectionDividerIncomeExpenseRow(
                    income = income,
                    expenses = expenses,
                    baseCurrency = baseCurrency
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        NivyIcon(
            modifier = Modifier.rotate(expandIconRotation),
            icon = R.drawable.ic_expandarrow
        )

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun SectionDividerIncomeExpenseRow(
    income: Double,
    expenses: Double,
    baseCurrency: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (expenses > 0) {
            Text(
                modifier = Modifier.testTag("upcoming_expense"),
                text = "${expenses.format(baseCurrency)} $baseCurrency",
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.expenses_lowercase),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        if (income > 0 && expenses > 0) {
            Spacer(Modifier.width(8.dp))

            NivyDividerDot()

            Spacer(Modifier.width(8.dp))
        }

        if (income > 0) {
            Text(
                modifier = Modifier.testTag("upcoming_income"),
                text = "${income.format(baseCurrency)} $baseCurrency",
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    // Readable on light pages; the text color in minimal colors.
                    color = incomeGreen(MaterialTheme.colorScheme.background).shown(),
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.income_lowercase),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

@Preview
@Composable
private fun Preview_Income_Expenses() {
    NivyComponentPreview {
        SectionDivider(
            expanded = true,
            title = "Upcoming",
            titleColor = Orange,
            baseCurrency = "BGN",
            income = 8043.23,
            expenses = 923.87
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_Expenses() {
    NivyComponentPreview {
        SectionDivider(
            expanded = true,
            title = "Overdue",
            titleColor = Palette.Red,
            baseCurrency = "BGN",
            income = 0.0,
            expenses = 923.87
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_Income() {
    NivyComponentPreview {
        SectionDivider(
            expanded = true,
            title = "Upcoming",
            titleColor = Orange,
            baseCurrency = "BGN",
            income = 8043.23,
            expenses = 0.0
        ) {
        }
    }
}
