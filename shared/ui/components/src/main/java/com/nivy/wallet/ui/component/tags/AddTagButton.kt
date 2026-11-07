package com.nivy.wallet.ui.component.tags

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.data.model.TagId
import com.nivy.design.system.colors.Orange3
import com.nivy.wallet.ui.theme.components.NivyBorderButton
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.design.system.colors.readableOn
import kotlinx.collections.immutable.ImmutableList
import com.nivy.ui.R
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
    val contrastColor = readableOn(Orange3)
    NivyButton(
        modifier = Modifier.padding(start = 24.dp),
        text = if (transactionTags.size <= 1) "${transactionTags.size}\t Tag" else "${transactionTags.size}\t Tags",
        backgroundColor = Orange3,
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

@Composable
private fun AddTagsButton(
    onClick: () -> Unit,
) {
    NivyBorderButton(
        modifier = Modifier.padding(start = 24.dp),
        iconStart = R.drawable.ic_plus,
        iconTint = MaterialTheme.colorScheme.onSurface,
        text = stringResource(R.string.add_tags),
        onClick = onClick
    )
}
