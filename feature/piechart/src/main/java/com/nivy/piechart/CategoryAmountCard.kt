package com.nivy.piechart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nivy.wallet.utils.format
import com.nivy.ui.R
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.wallet.ui.theme.components.ItemIconM
import com.nivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.toComposeColor
import com.nivy.wallet.ui.theme.wallet.AmountCurrencyB1Row
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberBodyLarge

@Composable
internal fun CategoryAmountCard(
    categoryAmount: CategoryAmount,
    rank: Int,
    currency: String,
    totalAmount: Double,

    selectedCategory: SelectedCategory?,

    onClick: () -> Unit
) {
    val category = categoryAmount.category
    val amount = categoryAmount.amount

    // In minimal colors, the slice's tone: the list is the chart's legend.
    val categoryColor = if (ColorMode.minimal) {
        pieTone(rank)
    } else {
        category?.color?.value?.toComposeColor() ?: Gray // Unspecified category = Gray
    }
    val selectedState = when {
        selectedCategory == null -> {
            // no selectedCategory
            false
        }

        categoryAmount.category == selectedCategory.category -> {
            // selectedCategory && we're selected
            true
        }

        else -> false
    }
    val backgroundColor = if (selectedState) categoryColor else MaterialTheme.colorScheme.surfaceVariant

    val textColor = readableOn(backgroundColor)

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor, MaterialTheme.shapes.large)
            .clickable {
                onClick()
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        ItemIconM(
            modifier = Modifier.background(categoryColor, PillShape),
            iconName = category?.icon?.id,
            tint = readableOn(categoryColor),
            iconContentScale = ContentScale.None,
            default = {
                ItemIconMDefaultIcon(
                    modifier = Modifier.background(categoryColor, PillShape),
                    iconName = category?.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_m,
                    tint = readableOn(categoryColor)
                )
            }
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    text = category?.name?.value ?: stringResource(R.string.unspecified),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                )

                PercentText(
                    amount = amount,
                    totalAmount = totalAmount,
                    selectedState = selectedState,
                    contrastColor = textColor
                )

                Spacer(Modifier.width(24.dp))
            }

            Spacer(Modifier.height(4.dp))

            AmountCurrencyB1Row(
                amount = amount,
                currency = currency,
                textColor = textColor,
                amountFontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PercentText(
    amount: Double,
    totalAmount: Double,
    selectedState: Boolean,
    contrastColor: Color
) {
    Text(
        text = if (totalAmount != 0.0) {
            stringResource(R.string.percent, ((amount / totalAmount) * 100).format(2))
        } else {
            stringResource(R.string.percent, "0")
        },
        style = MaterialTheme.typography.numberBodyLarge.copy(
            color = if (selectedState) contrastColor else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal
        )
    )
}
