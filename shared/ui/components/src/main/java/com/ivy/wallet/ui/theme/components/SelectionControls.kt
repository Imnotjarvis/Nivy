package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.White
import com.ivy.design.system.colors.readableTint
import com.ivy.design.system.shapes.PillShape
import com.ivy.design.utils.thenIf
import com.ivy.ui.R

// The controls that pick one of a few options, in one place so their selected state looks, and
// reads, the same everywhere.

/**
 * One option of a two-option switch, such as a loan's "Borrow" or "Lend": its label in a pill,
 * filled with the brand color when selected.
 */
@Composable
fun RowScope.SegmentedButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .weight(1f)
            .clip(PillShape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .thenIf(selected) {
                background(Ivy, PillShape)
            }
            .padding(vertical = 8.dp),
        text = label,
        style = MaterialTheme.typography.bodyLarge.copy(
            // Readable on the fill, also where minimal colors turn it into the text color.
            color = if (selected) readableTint(Ivy, White) else Gray,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    )
}

/**
 * An option in a list to choose from, such as a transaction's type: its icon and name across
 * the width, filled with [selectedFill] and marked "Selected" when it's the one chosen.
 */
@Composable
fun SelectableOptionRow(
    text: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    selectedFill: Color,
    modifier: Modifier = Modifier,
    selectedContent: Color = White,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                color = if (selected) selectedFill else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        // Readable on the selected fill, also where minimal colors turn it into the text color.
        val content = readableTint(selectedFill, selectedContent)
        val textColor = if (selected) content else MaterialTheme.colorScheme.onSurface

        IvyIcon(
            icon = icon,
            tint = textColor
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        )

        if (selected) {
            Spacer(Modifier.weight(1f))

            IvyIcon(
                icon = R.drawable.ic_check,
                tint = content
            )

            Text(
                text = stringResource(R.string.selected_text),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = content,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(Modifier.width(24.dp))
        }
    }
}
