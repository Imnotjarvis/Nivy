package com.ivy.legacy.ui.component.tags

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.data.model.TagId
import com.ivy.design.system.colors.Orange3
import com.ivy.design.system.colors.Gradient
import com.ivy.wallet.ui.theme.components.IvyBorderButton
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.design.system.colors.findContrastTextColor
import kotlinx.collections.immutable.ImmutableList
import com.ivy.ui.R
import androidx.compose.material3.MaterialTheme

@Composable
fun AddTagButton(
    transactionAssociatedTags: ImmutableList<TagId>,
    onClick: () -> Unit
) {
    if (transactionAssociatedTags.isNotEmpty()) {
        ViewTagsButton(transactionTags = transactionAssociatedTags, onClick = onClick)
    } else {
        AddTagsButton(onClick = onClick)
    }
}

@Composable
private fun ViewTagsButton(
    transactionTags: ImmutableList<TagId>,
    onClick: () -> Unit,
) {
    val contrastColor = findContrastTextColor(Orange3)
    IvyButton(
        modifier = Modifier.padding(start = 24.dp),
        text = if (transactionTags.size <= 1) "${transactionTags.size}\t Tag" else "${transactionTags.size}\t Tags",
        backgroundGradient = Gradient.solid(Orange3),
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

@Composable
private fun AddTagsButton(
    onClick: () -> Unit,
) {
    IvyBorderButton(
        modifier = Modifier.padding(start = 24.dp),
        iconStart = R.drawable.ic_plus,
        iconTint = MaterialTheme.colorScheme.onSurface,
        text = stringResource(R.string.add_tags),
        onClick = onClick
    )
}
