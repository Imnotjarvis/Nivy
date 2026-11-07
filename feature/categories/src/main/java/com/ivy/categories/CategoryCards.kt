package com.ivy.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.ivy.design.utils.outlineBehind
import com.ivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.wallet.utils.balancePrefix
import com.ivy.wallet.utils.compactBalancePrefix
import com.ivy.wallet.utils.format
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.design.system.colors.categoryColor
import com.ivy.design.system.colors.readableOn
import com.ivy.design.system.colors.incomeGreen
import com.ivy.design.system.colors.expenseRed
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.design.system.typography.numberTitleLarge
import androidx.compose.material3.VerticalDivider

@Composable
internal fun CategoryCard(
    currency: String,
    categoryData: CategoryData,
    compactModeEnabled: Boolean,
    onClick: () -> Unit
) {
    val contrastColor = readableOn(categoryColor(categoryData.category.color.value))

    if (!compactModeEnabled) {
        Spacer(Modifier.height(16.dp))
        DefaultCategoryCard(onClick, categoryData, currency)
    } else {
        Spacer(Modifier.height(8.dp))
        CompactCategoryCard(
            categoryData = categoryData,
            contrastColor = contrastColor,
            currency = currency,
            onClick = onClick
        )
    }
}

@Composable
private fun DefaultCategoryCard(
    onClick: () -> Unit,
    categoryData: CategoryData,
    currency: String
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            // Behind the content: the colored header covers it, so it frames only the rest.
            .outlineBehind(2.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(
                onClick = onClick
            )
    ) {
        CategoryHeader(
            categoryData = categoryData,
            currency = currency,
            contrastColor = readableOn(categoryColor(categoryData.category.color.value))
        )

        Spacer(Modifier.height(12.dp))

        // Emitting content
        AddedSpent(
            currency = currency,
            monthlyIncome = categoryData.monthlyIncome,
            monthlyExpenses = categoryData.monthlyExpenses
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CompactCategoryCard(
    categoryData: CategoryData,
    contrastColor: Color,
    currency: String,
    onClick: () -> Unit
) {
    val category = categoryData.category
    val balancePrefixValue = compactBalancePrefix(
        income = categoryData.monthlyIncome,
        expenses = categoryData.monthlyExpenses
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(
                onClick = onClick
            ),
    ) {
        Row(
            modifier = Modifier
                .padding(all = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(PillShape)
                    .background(categoryColor(category.color.value)),
                contentAlignment = Alignment.Center,
            ) {
                ItemIconSDefaultIcon(
                    iconName = category.icon?.id,
                    defaultIcon = R.drawable.ic_custom_account_s,
                    tint = contrastColor
                )
            }

            Row(
                modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A long name gives way to the amount: one line, cut with an ellipsis.
                Text(
                    modifier = Modifier.weight(1f),
                    text = category.name.value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Format the monthly balance according to the currency format and remove
                    // any '+' or '-' signs that might be included from the prefix to ensure
                    // a clean and consistent representation.
                    val currencyFormatted =
                        categoryData.monthlyBalance.format(currency).replace(Regex("[+-]"), "")

                    Text(
                        text = "$balancePrefixValue$currencyFormatted",
                        style = MaterialTheme.typography.numberTitleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.numberBodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AddedSpent(
    monthlyIncome: Double,
    monthlyExpenses: Double,
    currency: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    dividerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    center: Boolean = true,
    dividerSpacer: Dp? = null,

    ) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (center) {
            Spacer(Modifier.weight(1f))
        }

        LabelAmount(
            textColor = textColor,
            // Green and red as on Home's cards, also in minimal colors.
            amountColor = expenseRed(MaterialTheme.colorScheme.surface),
            label = stringResource(R.string.month_expenses),
            amount = monthlyExpenses,
            currency = currency,
            center = center
        )

        if (center) {
            Spacer(Modifier.weight(1f))
        }

        if (dividerSpacer != null) {
            Spacer(modifier = Modifier.width(dividerSpacer))
        }

        VerticalDivider(
            modifier = Modifier
                .height(48.dp)
                .clip(PillShape),
            thickness = 2.dp,
            color = dividerColor
        )

        if (center) {
            Spacer(Modifier.weight(1f))
        }

        if (dividerSpacer != null) {
            Spacer(modifier = Modifier.width(dividerSpacer))
        }

        LabelAmount(
            textColor = textColor,
            amountColor = incomeGreen(MaterialTheme.colorScheme.surface),
            label = stringResource(R.string.month_income),
            amount = monthlyIncome,
            currency = currency,
            center = center
        )

        if (center) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LabelAmount(
    label: String,
    amount: Double,
    currency: String,
    textColor: Color,
    amountColor: Color,
    center: Boolean
) {
    Column(
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AmountCurrencyB1(
                textColor = amountColor,
                amount = amount,
                currency = currency
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    categoryData: CategoryData,
    currency: String,
    contrastColor: Color,
) {
    val category = categoryData.category
    val balancePrefixValue = balancePrefix(
        income = categoryData.monthlyIncome,
        expenses = categoryData.monthlyExpenses
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(categoryColor(category.color.value))
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            ItemIconSDefaultIcon(
                iconName = category.icon?.id,
                defaultIcon = R.drawable.ic_custom_category_s,
                tint = contrastColor
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = category.name.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.height(4.dp))

        BalanceRow(
            modifier = Modifier.align(Alignment.CenterHorizontally),

            textColor = contrastColor,
            currency = currency,
            balance = categoryData.monthlyBalance,

            balanceFontSize = 30.sp,

            currencyUpfront = false,
            balanceAmountPrefix = balancePrefixValue
        )

        Spacer(Modifier.height(16.dp))
    }
}
