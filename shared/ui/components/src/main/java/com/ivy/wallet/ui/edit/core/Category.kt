package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Category
import com.ivy.ui.R
import com.ivy.design.system.colors.Gradient
import com.ivy.wallet.ui.theme.components.IvyBorderButton
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.getCustomIconIdS
import com.ivy.design.system.colors.findContrastTextColor
import com.ivy.design.system.colors.toComposeColor
import androidx.compose.material3.MaterialTheme

@Composable
fun Category(
    category: Category?,
    modifier: Modifier = Modifier,
    onChooseCategory: () -> Unit
) {
    Box(modifier = modifier) {
        if (category != null) {
            CategoryButton(category = category) {
                onChooseCategory()
            }
        } else {
            IvyBorderButton(
                modifier = Modifier.padding(start = 24.dp),
                iconStart = R.drawable.ic_plus,
                iconTint = MaterialTheme.colorScheme.onSurface,
                text = stringResource(R.string.add_category)
            ) {
                onChooseCategory()
            }
        }
    }
}

@Composable
private fun CategoryButton(
    category: Category,
    onClick: () -> Unit,
) {
    val contrastColor = findContrastTextColor(category.color.value.toComposeColor())
    IvyButton(
        modifier = Modifier.padding(start = 24.dp),
        text = category.name.value,
        iconStart = getCustomIconIdS(
            iconName = category.icon?.id,
            defaultIcon = R.drawable.ic_custom_category_s
        ),
        backgroundGradient = Gradient.from(category.color.value, category.color.value),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = contrastColor,
            fontWeight = FontWeight.Bold
        ),
        iconTint = contrastColor,
        hasGlow = false,
        iconEnd = R.drawable.ic_onboarding_next_arrow,
        wrapContentMode = true,
        onClick = onClick
    )
}
