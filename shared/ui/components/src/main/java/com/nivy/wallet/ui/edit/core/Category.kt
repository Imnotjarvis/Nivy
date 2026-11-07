package com.nivy.wallet.ui.edit.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Category
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.NivyBorderButton
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.getCustomIconIdS
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.categoryColor
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
            NivyBorderButton(
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
    val contrastColor = readableOn(categoryColor(category.color.value))
    NivyButton(
        modifier = Modifier.padding(start = 24.dp),
        text = category.name.value,
        iconStart = getCustomIconIdS(
            iconName = category.icon?.id,
            defaultIcon = R.drawable.ic_custom_category_s
        ),
        backgroundColor = categoryColor(category.color.value),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = contrastColor,
            fontWeight = FontWeight.Bold
        ),
        iconTint = contrastColor,
        iconEnd = R.drawable.ic_onboarding_next_arrow,
        wrapContentMode = true,
        onClick = onClick
    )
}
