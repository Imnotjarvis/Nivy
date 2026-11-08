package com.nivy.categories

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
import com.nivy.design.utils.outlineBehind
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivy.wallet.utils.balancePrefix
import com.nivy.wallet.utils.compactBalancePrefix
import com.nivy.wallet.utils.format
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.nivy.wallet.ui.theme.components.TwoAmountsRow
import com.nivy.design.system.colors.categoryColor
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.expenseRed
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.system.typography.numberTitleLarge

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
        TwoAmountsRow(
            startLabel = stringResource(R.string.month_expenses),
            startAmount = categoryData.monthlyExpenses,
            endLabel = stringResource(R.string.month_income),
            endAmount = categoryData.monthlyIncome,
            currency = currency,
            // Green and red as on Home's cards, also in minimal colors.
            startColor = expenseRed(MaterialTheme.colorScheme.surface),
            endColor = incomeGreen(MaterialTheme.colorScheme.surface),
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
                .padding(all = 12.dp),
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

            balanceAmountPrefix = balancePrefixValue
        )

        Spacer(Modifier.height(16.dp))
    }
}
