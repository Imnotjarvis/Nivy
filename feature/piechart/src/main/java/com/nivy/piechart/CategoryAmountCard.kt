package com.nivy.piechart

import androidx.annotation.DrawableRes
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
import com.nivy.wallet.utils.formatDecimals
import com.nivy.ui.R
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
    currency: String,
    totalAmount: Double,

    selectedCategory: SelectedCategory?,

    onClick: () -> Unit
) {
    val category = categoryAmount.category
    ShareCard(
        name = category?.name?.value ?: stringResource(R.string.unspecified),
        iconName = category?.icon?.id,
        defaultIcon = R.drawable.ic_custom_category_m,
        // Unspecified category = Gray
        color = category?.color?.value?.toComposeColor() ?: Gray,
        amount = categoryAmount.amount,
        totalAmount = totalAmount,
        currency = currency,
        selected = selectedCategory != null && category == selectedCategory.category,
        onClick = onClick,
    )
}

@Composable
internal fun AccountAmountCard(
    accountAmount: AccountAmount,
    currency: String,
    totalAmount: Double,
    selected: Boolean,
    onClick: () -> Unit
) {
    val account = accountAmount.account
    ShareCard(
        name = account.name.value,
        iconName = account.icon?.id,
        defaultIcon = R.drawable.ic_custom_account_m,
        color = account.color.value.toComposeColor(),
        amount = accountAmount.amount,
        totalAmount = totalAmount,
        currency = currency,
        selected = selected,
        onClick = onClick,
    )
}

// A slice's card under the chart: its icon in its color, its name, its share and its amount.
// Filled with its color while its slice is selected.
@Composable
private fun ShareCard(
    name: String,
    iconName: String?,
    @DrawableRes defaultIcon: Int,
    color: Color,
    amount: Double,
    totalAmount: Double,
    currency: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) color else MaterialTheme.colorScheme.surfaceVariant

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
            modifier = Modifier.background(color, PillShape),
            iconName = iconName,
            tint = readableOn(color),
            iconContentScale = ContentScale.None,
            default = {
                ItemIconMDefaultIcon(
                    modifier = Modifier.background(color, PillShape),
                    iconName = iconName,
                    defaultIcon = defaultIcon,
                    tint = readableOn(color)
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
                    text = name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                )

                PercentText(
                    amount = amount,
                    totalAmount = totalAmount,
                    selectedState = selected,
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
            stringResource(R.string.percent, ((amount / totalAmount) * 100).formatDecimals(2))
        } else {
            stringResource(R.string.percent, "0")
        },
        style = MaterialTheme.typography.numberBodyLarge.copy(
            color = if (selectedState) contrastColor else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal
        )
    )
}
