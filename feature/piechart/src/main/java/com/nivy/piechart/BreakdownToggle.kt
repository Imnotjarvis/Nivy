package com.nivy.piechart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.SegmentedButton

/** What the chart splits its total by. */
enum class Breakdown { Categories, Accounts }

/** Splits the chart by category or by account: the two in a track, the chosen one filled. */
@Composable
internal fun BreakdownToggle(
    breakdown: Breakdown,
    modifier: Modifier = Modifier,
    onSelect: (Breakdown) -> Unit,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge)
            .selectableGroup()
            .testTag("piechart_breakdown"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))

        SegmentedButton(
            label = stringResource(R.string.categories),
            selected = breakdown == Breakdown.Categories,
        ) {
            onSelect(Breakdown.Categories)
        }

        Spacer(Modifier.width(8.dp))

        SegmentedButton(
            label = stringResource(R.string.accounts),
            selected = breakdown == Breakdown.Accounts,
        ) {
            onSelect(Breakdown.Accounts)
        }

        Spacer(Modifier.width(8.dp))
    }
}
